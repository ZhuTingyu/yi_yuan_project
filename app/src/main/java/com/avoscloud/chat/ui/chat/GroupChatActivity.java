package com.avoscloud.chat.ui.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.callback.AVIMSingleMessageQueryCallback;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.controller.MessageAgent;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.model.MessageEvent;
import com.avoscloud.leanchatlib.view.ViewHolder;
import com.dimo.utils.StringUtil;
import com.squareup.picasso.Picasso;
import com.yuan.house.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by Alsor Zhou on 16/7/24.
 */

public class GroupChatActivity extends ChatActivity {
    private static final String PARAMS = "PARAMS";
    protected MessageAgent.SendCallback defaultSendCallback = new GroupChatActivity.DefaultSendCallback();
    Context mContext;
    ArrayList<AVIMConversation> conversations;
    ArrayList<GroupChatRowInfo> groupChatRowInfos;
    private JSONObject jsonFormatParams;
    private ChatGroupAdapter mGroupAdapter;
    private String[] leanIds;
    private String[] userIds;
    private ArrayList<String> convIds;
    private String cachedHouseIdForCurrentConv;
    private String cachedHouseTradeTypeForCurrentConv;
    private String cachedUserType;
    private String mLastMsgContent;

    public static void chatByUserIds(final Activity from, final JSONObject params) {
        Intent intent = new Intent(from, GroupChatActivity.class);
        intent.putExtra(PARAMS, params.toString());

        from.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        lvMessages.setVisibility(View.GONE);
        lvGroups.setVisibility(View.VISIBLE);

        conversations = new ArrayList<>();

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String raw = bundle.getString(PARAMS);
            try {
                jsonFormatParams = new JSONObject(raw);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        groupChatRowInfos = configGroupInfos(jsonFormatParams);

        bindAdapter(groupChatRowInfos);

        setTitleItem(R.string.title_group_chat);

        cachedHouseIdForCurrentConv = jsonFormatParams.optString("house_id");
        cachedHouseTradeTypeForCurrentConv = jsonFormatParams.optString("trade_type");
        cachedUserType = jsonFormatParams.optString("type");

        lvGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JSONObject object = new JSONObject();
                try {
                    object.put("lean_id", leanIds[position]);
                    object.put("user_id", userIds[position]);

                    SingleChatActivity.chatByUserId(mContext, object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        updateConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateLastMessageForConversation(null);
    }

    private void updateLastMessageForConversation(AVIMTypedMessage msg) {
        if (msg != null) {
            String convId = msg.getConversationId();

            int index = convIds.indexOf(convId);

            if (index >= 0) {
                groupChatRowInfos.get(index).setMessage(MessageHelper.outlineOfMsg(msg));
            }
        } else {
            for (AVIMConversation conv : conversations) {
                final String otherId = ConversationHelper.otherIdOfConv(conv);

                conv.getLastMessage(new AVIMSingleMessageQueryCallback() {
                    @Override
                    public void done(AVIMMessage avimMessage, AVIMException e) {
                        if (avimMessage == null) return;

                        Timber.v("leanId : " + avimMessage.getFrom());

                        for (GroupChatRowInfo info : groupChatRowInfos) {
                            if (info.getLeanId().equals(otherId)) {
                                info.setMessage(MessageHelper.outlineOfMsg((AVIMTypedMessage) avimMessage));
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * 获取该群聊里头的所有会话
     */
    private void updateConversations() {
        convIds = new ArrayList<>();

        for (int i = 0; i < leanIds.length; i++) {
            JSONObject params = new JSONObject();
            try {
                params.put("house_id", cachedHouseIdForCurrentConv);
                params.put("audit_type", cachedHouseTradeTypeForCurrentConv);
                params.put("type", cachedUserType);
                params.put("user_id", userIds[i]);

                final int finalI = i;
                ChatManager.getInstance().fetchConversationWithUserId(params, leanIds[i], new AVIMConversationCreatedCallback() {
                    @Override
                    public void done(AVIMConversation conversation, AVIMException e) {
                        if (Utils.filterException(e)) {
                            conversations.add(finalI, conversation);
//                            groupChatRowInfos.get(finalI).setConvId(conversation.getConversationId());
                            convIds.add(conversation.getConversationId());
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<GroupChatRowInfo> configGroupInfos(JSONObject jsonFormatParams) {
        String[] avatars = jsonFormatParams.optString("avatar").split(",");
        leanIds = jsonFormatParams.optString("lean_id").split(",");
        userIds = jsonFormatParams.optString("user_id").split(",");
        String[] names = jsonFormatParams.optString("name").split(",");

        ArrayList<GroupChatRowInfo> groups = new ArrayList<>();
        for (int i = 0; i < avatars.length; i++) {
            GroupChatRowInfo info = new GroupChatRowInfo(leanIds[i], avatars[i], names[i], null);
            groups.add(info);
        }

        return groups;
    }

    protected void bindAdapter(ArrayList<GroupChatRowInfo> datum) {
        bindAdapterToListView(datum);

//        refreshMsgsFromDB();
    }

    private void bindAdapterToListView(ArrayList<GroupChatRowInfo> datum) {
        mGroupAdapter = new ChatGroupAdapter(this, datum);
        lvGroups.setAdapter(mGroupAdapter);
    }

    public void onEvent(MessageEvent messageEvent) {
        AVIMTypedMessage msg = messageEvent.getMsg();

        updateLastMessageForConversation(msg);
    }

    @Override
    protected void sendText() {
        // TODO: 16/7/26 loop send text
        final String content = contentEdit.getText().toString();

        mLastMsgContent = content;

        if (!TextUtils.isEmpty(content)) {
            for (AVIMConversation conversation : conversations) {
                AVIMTextMessage message = new AVIMTextMessage();

                MessageAgent messageAgent = new MessageAgent(conversation);
                messageAgent.setSendCallback(defaultSendCallback);

                Map<String, Object> attrs = new HashMap<>();
                attrs.put("houseId", cachedHouseIdForCurrentConv);
                attrs.put("username", jsonFormatParams.optString("nickname"));
                message.setAttrs(attrs);

                message.setText(content);

                messageAgent.sendEncapsulatedTypedMessage(message);
            }

            contentEdit.setText("");
        }
    }

    @Override
    protected void sendAudio(final String audioPath) {
        super.sendAudio(audioPath);

        mLastMsgContent = "[语音]";

        if (!TextUtils.isEmpty(audioPath)) {
            for (AVIMConversation conversation : conversations) {
                MessageAgent messageAgent = new MessageAgent(conversation);
                messageAgent.setSendCallback(defaultSendCallback);
                messageAgent.sendAudio(audioPath);
            }
        }
    }

    @Override
    protected void sendImage(final String s) {
        super.sendImage(s);

        mLastMsgContent = "[图片]";

        if (!TextUtils.isEmpty(s)) {
            for (AVIMConversation conversation : conversations) {
                MessageAgent messageAgent = new MessageAgent(conversation);
                messageAgent.setSendCallback(defaultSendCallback);
                messageAgent.sendImage(s);
            }
        }
    }

    class DefaultSendCallback implements MessageAgent.SendCallback {
        @Override
        public void onError(Exception e) {
        }

        @Override
        public void onSuccess(AVIMTypedMessage msg) {
            // Send Callback, update last message
            updateLastMessageForConversation(msg);
        }
    }

    class GroupChatRowInfo {
        String convId;
        String leanId;
        String avatar;
        String name;
        String message;

        public GroupChatRowInfo(String leanId, String avatar, String name, String message) {
            this.leanId = leanId;
            this.avatar = avatar;
            this.name = name;
            this.message = message;
        }

        public String getLeanId() {
            return leanId;
        }

        public String getAvatar() {
            return avatar;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(CharSequence charSequence) {
            this.message = charSequence.toString();
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getConvId() {
            return convId;
        }

        public void setConvId(String convId) {
            this.convId = convId;
        }
    }

    public class ChatGroupAdapter extends BaseAdapter {
        private final ArrayList<GroupChatRowInfo> mCollection;
        Context context;

        public ChatGroupAdapter(Context context, ArrayList<GroupChatRowInfo> datum) {
            this.context = context;
            this.mCollection = datum;
        }

        @Override
        public int getCount() {
            return mCollection.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.chat_group_row, null);
            }

            ImageView ivAvatar = ViewHolder.findViewById(convertView, R.id.imgAvatar);
            TextView tvPrimary = ViewHolder.findViewById(convertView, R.id.tvPrimary);
            TextView tvSecondary = ViewHolder.findViewById(convertView, R.id.tvSecondary);

            GroupChatRowInfo info = mCollection.get(position);

            if (!TextUtils.isEmpty(info.getAvatar())) {
                if (StringUtil.isValidHTTPUrl(info.getAvatar())) {
                    // 加载远程图片
                    Picasso.with(context)
                            .load(info.getAvatar())
                            .placeholder(R.drawable.chat_default_user_avatar)
                            .into(ivAvatar);
                } else if (StringUtil.isValidPath(info.getAvatar())) {
                    // 加载本地图片
                    Uri uri = Uri.parse("android.resource://" + getApplication().getPackageName() + "/" + info.getAvatar());
                    Picasso.with(context)
                            .load(uri)
                            .placeholder(R.drawable.chat_default_user_avatar)
                            .into(ivAvatar);
                }
            } else {
                // 加载 placeholder
                Picasso.with(context)
                        .load(R.drawable.chat_default_user_avatar)
                        .into(ivAvatar);
            }

            tvPrimary.setText(info.getName());
            tvSecondary.setText(info.getMessage());

            return convertView;
        }
    }
}
