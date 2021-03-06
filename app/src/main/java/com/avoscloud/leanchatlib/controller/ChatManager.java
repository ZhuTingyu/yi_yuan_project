package com.avoscloud.leanchatlib.controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import com.avos.avoscloud.AVUser;
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
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.ui.chat.SingleChatActivity;
import com.avoscloud.chat.util.Utils;
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
import com.yuan.house.event.AuthEvent;
import com.yuan.house.helper.AuthHelper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by lzw on 15/2/10.
 */
public class ChatManager extends AVIMClientEventHandler {
    public static final String KEY_UPDATED_AT = "updatedAt";
    private static final long NOTIFY_PERIOD = 1000;
    private static ChatManager chatManager;
    private static long lastNotifyTime = 0;
    private static ConnectionListener defaultConnectListener = new ConnectionListener() {
        @Override
        public void onConnectionChanged(boolean connect) {
            // TODO: 8/11/16 reconnect if connection changed
            Logger.d("default connect listener");
        }
    };
    private static boolean setupDatabase = false;
    private ConnectionListener connectionListener = defaultConnectListener;
    private Map<String, AVIMConversation> cachedConversations = new HashMap<>();
    private AVIMClient imClient;
    private String selfId;
    private boolean connect = false;
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
        return DMApplication.getInstance();
    }

    /**
     * 根据聊天双方的信息以及房源 ID 查找已存在的会话, 或者创建新的会话
     *
     * @param param    房源信息, 带 ID
     * @param userId   对方用户 LeanMessage ID
     * @param callback 创建成功之后
     */
    public void fetchConversationWithUserId(final JSONObject param, final String userId, final AVIMConversationCreatedCallback callback) {
        final boolean isPeerTypeAgency = "agency".equals(param.optString("type"));

        String houseId = param.optString("house_id");
        if (TextUtils.isEmpty(houseId)) {
            houseId = param.optString("id");
        }
        int auditType = param.optInt("audit_type");
        if (auditType != 0) {
            houseId = String.format(Constants.kForceLocale, "000%d%s", auditType, houseId);
        }

        final List<String> members = new ArrayList<>();
        members.add(userId);
        members.add(selfId);
        AVIMConversationQuery query = imClient.getQuery();
        query.withMembers(members);
        query.whereEqualTo(ConversationType.ATTR_TYPE_KEY, ConversationType.Single.getValue());

        // find with houseId if user is `agency`
        if (isPeerTypeAgency && !AuthHelper.getInstance().iAmUser()) {
            query.whereEqualTo(ConversationType.ATTR_HOUSEID_KEY, houseId);
        }

        query.orderByDescending(KEY_UPDATED_AT);

        final String finalHouseId = houseId;
        final boolean finalIsPeerTypeAgency = isPeerTypeAgency;
        query.findInBackground(new AVIMConversationQueryCallback() {
            @Override
            public void done(List<AVIMConversation> conversations, AVIMException e) {
                if (e != null) {
                    callback.done(null, e);
                } else {
                    if (conversations.size() > 0) {
                        // 使用已存在的会话
                        Timber.w("FETCH CONV OLD - Peer Id : %s, House Id : %s, Conv Id : %s", userId, finalHouseId, conversations.get(0).getConversationId());

                        callback.done(conversations.get(0), null);
                    } else {
                        // 创建新会话
                        ArrayList<Integer> ids = new ArrayList<>();
                        ids.add(Integer.parseInt(AuthHelper.getInstance().getUserId()));
                        ids.add(Integer.parseInt(param.optString("user_id")));

                        Map<String, Object> attrs = new HashMap<>();
                        attrs.put(ConversationType.TYPE_KEY, ConversationType.Single.getValue());
                        if (!AuthHelper.getInstance().iAmUser()
                                && finalIsPeerTypeAgency
                                && !TextUtils.isEmpty(finalHouseId)) {
                            // 中介和中介聊天, 会绑定 house Id
                            attrs.put("houseId", finalHouseId);
                        }

                        attrs.put("userIds", ids);

                        attrs.put(userId, finalIsPeerTypeAgency ? "agency" : "user");
                        attrs.put(selfId, !AuthHelper.getInstance().iAmUser() ? "agency" : "user");

                        Timber.w("FETCH CONV NEW - Peer Id : %s, House Id : %s", userId, finalHouseId);

                        imClient.createConversation(members, attrs, callback);
                    }
                }
            }
        });
    }

    public void showMessageNotification(Context context, AVIMConversation conv, AVIMTypedMessage msg) {
        if (System.currentTimeMillis() - lastNotifyTime < NOTIFY_PERIOD) {
            return;
        } else {
            lastNotifyTime = System.currentTimeMillis();
        }

        CharSequence notifyContent = MessageHelper.outlineOfMsg(msg);
        CharSequence username = "username";
        UserInfo from = getUserInfoFactory().getUserInfoById(msg.getFrom());
        if (from != null) {
            username = from.getUsername();
        }

        Notification notification = Utils.notifyMsg(context, PackageUtil.getAppLable(context), username + "\n" + notifyContent, notifyContent.toString(), Constants.kNotifyId);
        getUserInfoFactory().configureNotification(notification);
    }

    public void init() {
        AVIMHouseMessage.registerMessageType();

        MsgHandler msgHandler = new MsgHandler();
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
        if (TextUtils.isEmpty(selfId)) {
            EventBus.getDefault().post(new AuthEvent(AuthEvent.AuthEventEnum.NEED_LOGIN_AGAIN, null));
        }
        return selfId;
    }

    public void avLogin() {
        if (AVUser.getCurrentUser() == null) {
            return;
        }
        setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
        openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);
        CacheService.registerUser(AVUser.getCurrentUser());
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

    /**
     * 收到 LM 消息的处理函数
     *
     * @param message      收到的消息
     * @param conversation 对应的会话
     */
    private void onMessage(final AVIMTypedMessage message, final AVIMConversation conversation) {
        Logger.d("receive message=" + message.getContent());
        if (message.getMessageId() == null) {
            throw new NullPointerException("message id is null");
        }
        if (!ConversationHelper.isValidConv(conversation)) {
            Timber.e("receive msg from invalid conversation");
//            throw new IllegalStateException("receive msg from invalid conversation");
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
                // 接收消息永远只会是单聊
                if (selfId != null && (
                        SingleChatActivity.getCurrentChattingConvid() == null ||
                                TextUtils.isEmpty(SingleChatActivity.getCurrentChattingConvid()) ||
                                !SingleChatActivity.getCurrentChattingConvid().equals(message.getConversationId()))
                        ) {
                    // Dismiss if message is Presence Message
                    if (message.getClass().equals(AVIMPresenceMessage.class)) return;

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
        String auditType = "0";
        String houseId = "";
        String text;

        HouseMessageType msgType = HouseMessageType.getMessageType(msg.getMessageType());
        text = MessageHelper.outlineOfMsg(msg).toString();

        Map<String, Object> objectMap = null;

        // FIXME: 8/16/16 shit code!!!
        if (msgType == HouseMessageType.TextMessageType) {
            objectMap = ((AVIMTextMessage) msg).getAttrs();
            text = ((AVIMTextMessage) msg).getText();
        } else if (msgType == HouseMessageType.HouseMessageType) {
            objectMap = ((AVIMHouseMessage) msg).getAttrs();
        } else if (msgType == HouseMessageType.AudioMessageType) {
            objectMap = ((AVIMAudioMessage) msg).getAttrs();
        } else if (msgType == HouseMessageType.ImageMessageType) {
            objectMap = ((AVIMImageMessage) msg).getAttrs();
        }

        if (objectMap != null) {
            houseId = objectMap.get("houseId").toString();
            if (objectMap.get("auditType") != null) {
                if (!StringUtils.isEmpty(objectMap.get("auditType").toString())) {
                    auditType = objectMap.get("auditType").toString();
                }
            }
        }

        if (params != null) {
            auditType = params.optString("audit_type");
            leanId = params.optString("lean_id");
        } else {
            leanId = msg.getFrom();
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
