package com.avoscloud.leanchatlib.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMClientEventHandler;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMConversationEventHandler;
import com.avos.avoscloud.im.v2.AVIMConversationQuery;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.AVIMTypedMessageHandler;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationQueryCallback;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.db.MsgsTable;
import com.avoscloud.leanchatlib.db.RoomsTable;
import com.avoscloud.leanchatlib.model.AVIMHouseMessage;
import com.avoscloud.leanchatlib.model.AVIMPresenceMessage;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.model.MessageEvent;
import com.avoscloud.leanchatlib.model.Room;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.avoscloud.leanchatlib.utils.Logger;
import com.avoscloud.leanchatlib.utils.NetAsyncTask;
import com.dimo.utils.DateUtil;
import com.dimo.utils.PackageUtil;
import com.lfy.bean.Message;
import com.lfy.dao.MessageDao;
import com.yuan.house.HouseMessageType;
import com.yuan.house.application.DMApplication;
import com.yuan.house.common.Constants;
import com.yuan.house.helper.AuthHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by lzw on 15/2/10.
 */
public class ChatManager extends AVIMClientEventHandler {
    public static final String KEY_UPDATED_AT = "updatedAt";
    private static final long NOTIFY_PERIOD = 1000;
    private static ChatManager chatManager;
    private static long lastNotifyTime = 0;
    private static Context context;
    private static ConnectionListener defaultConnectListener = new ConnectionListener() {
        @Override
        public void onConnectionChanged(boolean connect) {
            Logger.d("default connect listener");
        }
    };
    private static boolean setupDatabase = false;
    private ConnectionListener connectionListener = defaultConnectListener;
    private Map<String, AVIMConversation> cachedConversations = new HashMap<>();
    private AVIMClient imClient;
    private String selfId;
    private boolean connect = false;
    private MsgHandler msgHandler;
    private MsgsTable msgsTable;
    private RoomsTable roomsTable;
    private EventBus eventBus = EventBus.getDefault();
    private UserInfoFactory userInfoFactory;

    private ChatManager() {
    }

    public static synchronized ChatManager getInstance() {
        if (chatManager == null) {
            chatManager = new ChatManager();
        }
        return chatManager;
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Deprecated .
     */
    public void fetchConversationWithUserId(final String param, String userId, final AVIMConversationCreatedCallback callback) {
        final List<String> members = new ArrayList<>();
        members.add(userId);
        members.add(selfId);
        AVIMConversationQuery query = imClient.getQuery();
        query.withMembers(members);
        query.whereEqualTo(ConversationType.ATTR_TYPE_KEY, ConversationType.Single.getValue());
        query.orderByDescending(KEY_UPDATED_AT);

        query.findInBackground(new AVIMConversationQueryCallback() {
            @Override
            public void done(List<AVIMConversation> conversations, AVIMException e) {
                if (e != null) {
                    callback.done(null, e);
                } else {
                    if (conversations.size() > 0) {
                        callback.done(conversations.get(0), null);
                    } else {
                        Map<String, Object> attrs = new HashMap<>();
                        attrs.put(ConversationType.TYPE_KEY, ConversationType.Single.getValue());
                        attrs.put("houseId", param);
                        imClient.createConversation(members, attrs, callback);
                    }
                }
            }
        });
    }

    public void fetchConversationWithUserId(final JSONObject param, final String userId, final AVIMConversationCreatedCallback callback) {
        String houseId = param.optString("house_id");
        int auditType = param.optInt("audit_type");
        if (auditType != 0) {
            houseId = String.format("000%d%s", auditType, houseId);
        }

        final List<String> members = new ArrayList<>();
        members.add(userId);
        members.add(selfId);
        AVIMConversationQuery query = imClient.getQuery();
        query.withMembers(members);
        query.whereEqualTo(ConversationType.ATTR_TYPE_KEY, ConversationType.Single.getValue());

        // find with houseId if user is `agency`
        if ("agency".equals(param.optString("type"))) {
            query.whereEqualTo(ConversationType.ATTR_HOUSEID_KEY, houseId);
        }

        query.orderByDescending(KEY_UPDATED_AT);

        final String finalHouseId = houseId;
        query.findInBackground(new AVIMConversationQueryCallback() {
            @Override
            public void done(List<AVIMConversation> conversations, AVIMException e) {
                if (e != null) {
                    callback.done(null, e);
                } else {
                    if (conversations.size() > 0) {
                        callback.done(conversations.get(0), null);
                    } else {
                        ArrayList<Integer> ids = new ArrayList<>();
                        ids.add(Integer.parseInt(AuthHelper.getInstance().getUserId()));
                        ids.add(Integer.parseInt(param.optString("user_id")));

                        Map<String, Object> attrs = new HashMap<>();
                        attrs.put(ConversationType.TYPE_KEY, ConversationType.Single.getValue());
                        attrs.put("houseId", finalHouseId);
                        attrs.put("userIds", ids);

                        imClient.createConversation(members, attrs, callback);
                    }
                }
            }
        });
    }

    public void showMessageNotification(Context context, AVIMConversation conv, AVIMTypedMessage msg) {
        // Dismiss if message is Presence Message
        if (msg.getClass().equals(AVIMPresenceMessage.class)) return;

        if (System.currentTimeMillis() - lastNotifyTime < NOTIFY_PERIOD) {
            return;
        } else {
            lastNotifyTime = System.currentTimeMillis();
        }

        int icon = context.getApplicationInfo().icon;
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.CONVID, conv.getConversationId());

        CharSequence notifyContent = MessageHelper.outlineOfMsg(msg);
        CharSequence username = "username";
        UserInfo from = getUserInfoFactory().getUserInfoById(msg.getFrom());
        if (from != null) {
            username = from.getUsername();
        }

        Notification notification = Utils.notifyMsg(context, ChatActivity.class, PackageUtil.getAppLable(context), username + "\n" + notifyContent, notifyContent.toString(), Constants.kNotifyId);
        getUserInfoFactory().configureNotification(notification);
    }

    public void init(Context context) {
        this.context = context;

        AVIMHouseMessage.registerMessageType();

        msgHandler = new MsgHandler();
        AVIMMessageManager.registerMessageHandler(AVIMTypedMessage.class, msgHandler);

//    try {
//      AVIMMessageManager.registerAVIMMessageType(AVIMUserInfoMessage.class);
//    } catch (AVException e) {
//      e.printStackTrace();
//    }

        AVIMClient.setClientEventHandler(this);

        //签名
        //AVIMClient.setSignatureFactory(new SignatureFactory());
    }

    public void setConversationEventHandler(AVIMConversationEventHandler eventHandler) {
        AVIMMessageManager.setConversationEventHandler(eventHandler);
    }

    public void setupDatabaseWithSelfId(String selfId) {
        this.selfId = selfId;
        if (setupDatabase) {
            return;
        }
        setupDatabase = true;
        msgsTable = MsgsTable.getCurrentUserInstance();
        roomsTable = RoomsTable.getCurrentUserInstance();
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void cancelNotification() {
        NotificationManager nMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(Constants.kNotifyId);
    }

    public AVIMClient getImClient() {
        return imClient;
    }

    public String getSelfId() {
        return selfId;
    }

    public void openClientWithSelfId(String selfId, final AVIMClientCallback callback) {
        if (this.selfId == null) {
            throw new IllegalStateException("please call setupDatabaseWithSelfId() first");
        }
        if (!this.selfId.equals(selfId)) {
            throw new IllegalStateException("setupDatabaseWithSelfId and openClient's selfId should be equal");
        }
        imClient = AVIMClient.getInstance(selfId);
        imClient.open(new AVIMClientCallback() {
            @Override
            public void done(AVIMClient client, AVIMException e) {
                if (e != null) {
                    connect = false;
                    connectionListener.onConnectionChanged(connect);
                } else {
                    connect = true;
                    connectionListener.onConnectionChanged(connect);
                }
                if (callback != null) {
                    callback.done(client, e);
                }
            }
        });
    }

    private void onMessageReceipt(AVIMTypedMessage message, AVIMConversation conv) {
        if (message.getMessageId() == null) {
            throw new NullPointerException("message id is null");
        }
        msgsTable.updateStatus(message.getMessageId(), message.getMessageStatus());
        MessageEvent messageEvent = new MessageEvent(message);
        eventBus.post(messageEvent);
    }

    private void onMessage(final AVIMTypedMessage message, final AVIMConversation conversation) {
        Logger.d("receive message=" + message.getContent());
        if (message.getMessageId() == null) {
            throw new NullPointerException("message id is null");
        }
        if (!ConversationHelper.isValidConv(conversation)) {
            throw new IllegalStateException("receive msg from invalid conversation");
        }
        if (lookUpConversationById(conversation.getConversationId()) == null) {
            registerConversation(conversation);
        }

        if (!message.getClass().equals(AVIMPresenceMessage.class)) {
            msgsTable.insertMsg(message);
            roomsTable.insertRoom(message.getConversationId());
            roomsTable.increaseUnreadCount(message.getConversationId());

            storeLastMessage(message);
        }

        MessageEvent messageEvent = new MessageEvent(message);
        eventBus.post(messageEvent);

        new NetAsyncTask(getContext(), false) {
            @Override
            protected void doInBack() throws Exception {
                getUserInfoFactory().cacheUserInfoByIdsInBackground(Arrays.asList(message.getFrom()));
            }

            @Override
            protected void onPost(Exception exception) {
                if (selfId != null && ChatActivity.getCurrentChattingConvid() == null || TextUtils.isEmpty(ChatActivity.getCurrentChattingConvid()) || !ChatActivity.getCurrentChattingConvid().equals(message.getConversationId())) {
                    if (getUserInfoFactory().showNotificationWhenNewMessageCome(selfId)) {
                        showMessageNotification(getContext(), conversation, message);
                    }
                }
            }
        }.execute();
    }

    // FIXME: 16/6/27 workaround,目前是在本地消息存储之外,另行存储最近的消息,理想情况是直接查msgtable
    public void storeLastMessage(AVIMTypedMessage msg) {
        storeLastMessage(msg, null);
    }

    public void storeLastMessage(AVIMTypedMessage msg, JSONObject params) {
        String leanId;

        String auditType = null;
        String houseId = "";
        String text;

        if (params != null) {
            auditType = params.optString("audit_type");
            leanId = params.optString("lean_id");
        } else {
            leanId = msg.getFrom();
        }

        if (TextUtils.isEmpty(auditType)) auditType = "0";

        HouseMessageType msgType = HouseMessageType.getMessageType(msg.getMessageType());
        text = MessageHelper.outlineOfMsg(msg).toString();

        if (msgType == HouseMessageType.TextMessageType) {
            houseId = ((AVIMTextMessage) msg).getAttrs().get("houseId").toString();
            text = ((AVIMTextMessage) msg).getText();
        } else if (msgType == HouseMessageType.HouseMessageType) {
            houseId = ((AVIMHouseMessage) msg).getAttrs().get("houseId").toString();
        }

        MessageDao dao = DMApplication.getInstance().getMessageDao();
        Message message = dao.queryBuilder().where(
                MessageDao.Properties.HouseId.eq(houseId),
                MessageDao.Properties.AuditType.eq(auditType),
                MessageDao.Properties.LeanId.eq(leanId)
        ).unique();

        String dateString = DateUtil.toDateString(new Date(msg.getTimestamp()), Constants.kDateFormatStyleShort);
        if (message == null) {
            message = new Message();
            message.setHouseId(houseId);
            message.setAuditType(auditType);
            message.setLeanId(leanId);
        }

        if (params == null) {
            message.setIs_read(false);
        } else {
            message.setIs_read(true);
        }
        message.setMessage(text);

        message.setDate(dateString);

        dao.insertOrReplace(message);
    }

    public void closeWithCallback(final AVIMClientCallback callback) {
        if (imClient == null) return;

        imClient.close(new AVIMClientCallback() {

            @Override
            public void done(AVIMClient client, AVIMException e) {
                if (e != null) {
                    Logger.d(e.getMessage());
                }
                if (callback != null) {
                    callback.done(client, e);
                }
            }
        });
        imClient = null;
        selfId = null;
    }

    public AVIMConversationQuery getQuery() {
        return imClient.getQuery();
    }

    @Override
    public void onConnectionPaused(AVIMClient client) {
        Logger.d("connect paused");
        connect = false;
        connectionListener.onConnectionChanged(connect);
    }

    @Override
    public void onConnectionResume(AVIMClient client) {
        Logger.d("connect resume");
        connect = true;
        connectionListener.onConnectionChanged(connect);
    }

    @Override
    public void onClientOffline(AVIMClient avimClient, int i) {

    }

    public boolean isConnect() {
        return connect;
    }

    //cache
    public void registerConversation(AVIMConversation conversation) {
        cachedConversations.put(conversation.getConversationId(), conversation);
    }

    public AVIMConversation lookUpConversationById(String conversationId) {
        return cachedConversations.get(conversationId);
    }

    public UserInfoFactory getUserInfoFactory() {
        return userInfoFactory;
    }

    public void setUserInfoFactory(UserInfoFactory userInfoFactory) {
        this.userInfoFactory = userInfoFactory;
    }

    //ChatUser
    public List<Room> findRecentRooms() {
        RoomsTable roomsTable = RoomsTable.getCurrentUserInstance();
        return roomsTable.selectRooms();
    }

    public interface ConnectionListener {
        void onConnectionChanged(boolean connect);
    }

    private static class MsgHandler extends AVIMTypedMessageHandler<AVIMTypedMessage> {

        @Override
        public void onMessage(AVIMTypedMessage message, AVIMConversation conversation,
                              AVIMClient client) {
            chatManager.onMessage(message, conversation);
        }

        @Override
        public void onMessageReceipt(AVIMTypedMessage message, AVIMConversation conversation,
                                     AVIMClient client) {
            chatManager.onMessageReceipt(message, conversation);
        }
    }
}
