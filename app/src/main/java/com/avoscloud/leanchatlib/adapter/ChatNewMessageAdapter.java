package com.avoscloud.leanchatlib.adapter;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMReservedMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.leanchatlib.controller.AudioHelper;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.EmotionHelper;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.model.AVIMHouseMessage;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.avoscloud.leanchatlib.utils.PhotoUtils;
import com.avoscloud.leanchatlib.view.PlayButton;
import com.avoscloud.leanchatlib.view.ViewHolder;
import com.dimo.utils.DateUtil;
import com.lfy.bean.Message;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Picasso;
import com.yuan.house.R;

import java.util.Map;

import butterknife.ButterKnife;

/**
 * Created by edwardliu on 16/6/30.
 */
public class ChatNewMessageAdapter extends BaseListAdapter<AVIMTypedMessage> {

    protected org.json.JSONObject conversationObject;
    private ConversationType conversationType;
    private int msgViewTypes = 9;
    private ChatNewMessageAdapter.ClickListener clickListener;
    private Context context;

    private View contentLayout;
    private View placeView;
    private Activity activity;
    String peerAvatar;

    public ChatNewMessageAdapter(Context context, ConversationType conversationType, org.json.JSONObject object) {
        super(context);

        this.context = context;
        this.conversationType = conversationType;
        this.conversationObject = object;
        activity = (Activity) context;
        peerAvatar = object.optString("avatar");
    }

    public void setClickListener(ChatNewMessageAdapter.ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemViewType(int position) {
        AVIMTypedMessage msg = datas.get(position);
        boolean comeMsg = messageSentByOthers(msg);

        MsgViewType viewType = null;
        AVIMReservedMessageType msgType = AVIMReservedMessageType.getAVIMReservedMessageType(msg.getMessageType());
        switch (msgType) {
            case TextMessageType:
                viewType = comeMsg ? MsgViewType.ComeText : MsgViewType.ToText;
                break;
            case ImageMessageType:
                viewType = comeMsg ? MsgViewType.ComeImage : MsgViewType.ToImage;
                break;
            case AudioMessageType:
                viewType = comeMsg ? MsgViewType.ComeAudio : MsgViewType.ToAudio;
                break;
            case LocationMessageType:
                viewType = comeMsg ? MsgViewType.ComeLocation : MsgViewType.ToLocation;
                break;
            default:
                if (msg.getMessageType() == 2) {
                    viewType = comeMsg ? MsgViewType.ChangeHouse : MsgViewType.ChangeHouse;
                }
        }
        return viewType.getValue();
    }

    @Override
    public int getViewTypeCount() {
        return msgViewTypes;
    }

    protected boolean messageSentByOthers(AVIMTypedMessage msg) {
        return !MessageHelper.fromMe(msg);
    }

    public View getView(int position, View conView, ViewGroup parent) {
        AVIMMessage msg = datas.get(position);

        boolean others = false;

        if (conView == null) {
            Message bean = new Message();
            // FIXME: 16/6/27 date format error
            bean.setDate(String.valueOf(msg.getTimestamp()));
            bean.setLeanId(conversationObject.optString("lean_id"));
            bean.setAuditType(conversationObject.optString("audit_type"));
            bean.setHouseId(conversationObject.optString("house_id"));
            bean.setIs_read(true);

            if (msg instanceof AVIMHouseMessage) {
                AVIMHouseMessage houseInfoMessage = (AVIMHouseMessage) msg;
                others = messageSentByOthers(houseInfoMessage);
                conView = createViewByType(houseInfoMessage.getMessageType(), others);
                initHouseMessageView(conView, houseInfoMessage, others);
                bean.setMessage("[房源信息]");
            }
            else if (msg instanceof AVIMTypedMessage) {
                AVIMTypedMessage typedMessage = (AVIMTypedMessage) msg;
                others = messageSentByOthers(typedMessage);
                conView = createViewByType(AVIMReservedMessageType.getAVIMReservedMessageType(typedMessage.getMessageType()), others);
                initReservedMessageView(conView, position, typedMessage, others, bean);
            }
        }

        setSendTimeView(conView, position, msg);
        ImageView avatar = ButterKnife.findById(conView, R.id.avatar);

        if (others == true && peerAvatar != null) {
            Picasso.with(context).load(peerAvatar).placeholder(R.drawable.photo_agent_boy).into(avatar);
        }

//        activity.registerForContextMenu(contentLayout);

        return conView;
    }

    private void initHouseMessageView(View conView, AVIMTypedMessage msg, boolean isMessageSentByMe) {
        AVIMHouseMessage message = (AVIMHouseMessage) msg;

        Map<String, Object> map = message.getAttrs();
        JSONObject object = (JSONObject) JSON.toJSON(map);

        if (object.size() == 0) return;

        View houseView = ViewHolder.findViewById(conView, R.id.houseRL);
        ImageView img = ViewHolder.findViewById(conView, R.id.img);
        TextView title = ViewHolder.findViewById(conView, R.id.title);
        TextView area = ViewHolder.findViewById(conView, R.id.area);
//        TextView house_params = ViewHolder.findViewById(conView, R.id.house_params);
//        house_params.setVisibility(View.GONE);

        View statusSendFailed = ViewHolder.findViewById(conView, R.id.status_send_failed);
        View statusSendStart = ViewHolder.findViewById(conView, R.id.status_send_start);

        String imageUrl = object.getString("houseImage");
        Picasso.with(ctx).load(imageUrl).placeholder(R.drawable.img_placeholder).into(img);

        String houseName = object.getString("houseName");
        if (TextUtils.isEmpty(houseName)) houseName = "需求";
        title.setText(houseName);

        area.setText(object.getString("houseAddress"));

        if (isMessageSentByMe == false) {
            hideStatusViews(statusSendStart, statusSendFailed);
            setSendFailedBtnListener(statusSendFailed, msg);
            switch (msg.getMessageStatus()) {
                case AVIMMessageStatusFailed:
                    statusSendFailed.setVisibility(View.VISIBLE);
                    break;
                case AVIMMessageStatusSent:
                    break;
                case AVIMMessageStatusNone:
                case AVIMMessageStatusSending:
                    statusSendStart.setVisibility(View.VISIBLE);
                    break;
                case AVIMMessageStatusReceipt:
                    break;
            }
        }
        activity.registerForContextMenu(houseView);
    }

    /**
     * 设置时间显示栏
     * @param conView
     * @param position
     * @param msg
     */
    protected void setSendTimeView(View conView, int position, AVIMMessage msg) {
        TextView sendTimeView = ViewHolder.findViewById(conView, R.id.sendTimeView);
        if (position == 0 || DateUtil.haveTimeGap(datas.get(position - 1).getTimestamp(),
                msg.getTimestamp())) {
            sendTimeView.setVisibility(View.VISIBLE);
            sendTimeView.setText(DateUtil.getDate(msg.getTimestamp()));
        } else {
            sendTimeView.setVisibility(View.GONE);
        }
    }

    protected void initReservedMessageView(View conView, int position, AVIMTypedMessage msg, boolean isComMsg, Message message) {

        TextView contentView = ViewHolder.findViewById(conView, R.id.textContent);
        contentLayout = ViewHolder.findViewById(conView, R.id.contentLayout);
        ImageView imageView = ViewHolder.findViewById(conView, R.id.imageView);
        ImageView avatarView = ViewHolder.findViewById(conView, R.id.avatar);
        final PlayButton playBtn = ViewHolder.findViewById(conView, R.id.playBtn);
        TextView timeAudio = ViewHolder.findViewById(conView, R.id.dur_time);
        placeView = ViewHolder.findViewById(conView, R.id.audioPlaceView);
        View audioView = ViewHolder.findViewById(conView, R.id.audioLL);
        TextView locationView = ViewHolder.findViewById(conView, R.id.locationView);
        TextView usernameView = ViewHolder.findViewById(conView, R.id.username);

        View statusSendFailed = ViewHolder.findViewById(conView, R.id.status_send_failed);
        View statusSendStart = ViewHolder.findViewById(conView, R.id.status_send_start);

        if (null != placeView) {
            placeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playBtn.performClick();
                }
            });
        }


        UserInfo user = ChatManager.getInstance().getUserInfoFactory().getUserInfoById(msg.getFrom());
        if (user == null) {
            throw new NullPointerException("user is null");
        }
        if (isComMsg) {
            if (conversationType == null) {
                throw new NullPointerException("conv type is null");
            }
        }
        ImageLoader.getInstance().displayImage(user.getAvatarUrl(), avatarView, PhotoUtils.avatarImageOptions);

        AVIMReservedMessageType type = AVIMReservedMessageType.getAVIMReservedMessageType(msg.getMessageType());
        switch (type) {
            case TextMessageType:
                AVIMTextMessage textMsg = (AVIMTextMessage) msg;
                contentView.setText(EmotionHelper.replace(ChatManager.getContext(), textMsg.getText()));
                message.setMessage(textMsg.getText());
                activity.registerForContextMenu(contentView);
                break;
            case ImageMessageType:
                AVIMImageMessage imageMsg = (AVIMImageMessage) msg;
                /*PhotoUtils.displayImageCacheElseNetwork(imageView, MessageHelper.getFilePath(imageMsg),
                        imageMsg.getFileUrl());
                setImageOnClickListener(imageView, imageMsg);*/
                initImageView(imageView, imageMsg);
                message.setMessage("[图片]");
                activity.registerForContextMenu(imageView);
                break;
            case AudioMessageType:
                AVIMAudioMessage audioMessage = (AVIMAudioMessage) msg;
                initPlayBtn(msg, playBtn, audioMessage, timeAudio);
                message.setMessage("[语音]");
                activity.registerForContextMenu(audioView);
                break;
            case LocationMessageType:
                setLocationView(msg, locationView);
                message.setMessage("[位置]");
                activity.registerForContextMenu(locationView);
                break;
            default:
                break;
        }
        if (isComMsg == false) {
            hideStatusViews(statusSendStart, statusSendFailed);
            setSendFailedBtnListener(statusSendFailed, msg);
            switch (msg.getMessageStatus()) {
                case AVIMMessageStatusFailed:
                    statusSendFailed.setVisibility(View.VISIBLE);
                    break;
                case AVIMMessageStatusSent:
                    break;
                case AVIMMessageStatusNone:
                case AVIMMessageStatusSending:
                    statusSendStart.setVisibility(View.VISIBLE);
                    break;
                case AVIMMessageStatusReceipt:
                    break;
            }
        }
    }

    private void setSendFailedBtnListener(View statusSendFailed, final AVIMTypedMessage msg) {
        statusSendFailed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onFailButtonClick(msg);
                }
            }
        });
    }

    private void hideStatusViews(View statusSendStart, View statusSendFailed) {
        statusSendFailed.setVisibility(View.GONE);
        statusSendStart.setVisibility(View.GONE);
    }

    public void setLocationView(AVIMTypedMessage msg, TextView locationView) {
        final AVIMLocationMessage locMsg = (AVIMLocationMessage) msg;
        locationView.setText(locMsg.getText());
        locationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (clickListener != null) {
                    clickListener.onLocationViewClick(locMsg);
                }
            }
        });
    }

    protected void initImageView(ImageView imageView, AVIMImageMessage imageMsg) {
        PhotoUtils.displayImageCacheElseNetwork(imageView, MessageHelper.getFilePath(imageMsg),
                imageMsg.getFileUrl());
        setImageOnClickListener(imageView, imageMsg);
    }

    protected void initPlayBtn(AVIMTypedMessage msg, PlayButton playBtn, AVIMAudioMessage audioMessage, TextView timeAudio) {
        playBtn.setLeftSide(messageSentByOthers(msg));
        AudioHelper audioHelper = AudioHelper.getInstance();
        playBtn.setAudioHelper(audioHelper);
        playBtn.setPath(MessageHelper.getFilePath(msg));
        Object obj = audioMessage.getFileMetaData().get("duration");
        int time = (int) Double.parseDouble(obj.toString());
        setContentLayoutLength(time);
        timeAudio.setText(String.valueOf(time) + "''");
        setItemOnLongClickListener(playBtn, audioMessage);
    }

    private void setItemOnLongClickListener(PlayButton playBtn, final AVIMAudioMessage audioMessage) {
        playBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (clickListener != null) {
                    clickListener.onAudioLongClick(audioMessage);
                }
                return true;
            }
        });
    }

    protected void setImageOnClickListener(ImageView imageView, final AVIMImageMessage imageMsg) {
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onImageViewClick(imageMsg);
                }
            }
        });
        //让imageView不拦截长按事件
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
    }


    public View createViewByType(AVIMReservedMessageType type, boolean comeMsg) {
        View baseView = null;
        switch (type) {
            case TextMessageType:
                baseView = inflater.inflate(comeMsg ? R.layout.chat_text_receive : R.layout.chat_text_send, null);
                break;
            case AudioMessageType:
                baseView = inflater.inflate(comeMsg ? R.layout.chat_audio_receive : R.layout.chat_audio_send, null);
                break;
            case ImageMessageType:
                baseView = inflater.inflate(comeMsg ? R.layout.chat_image_receive : R.layout.chat_image_send, null);
                break;
            case LocationMessageType:
                baseView = inflater.inflate(comeMsg ? R.layout.chat_location_receive : R.layout.chat_location_send, null);
                break;
            default:
                throw new IllegalStateException();
        }

        if (type == AVIMReservedMessageType.AudioMessageType) {
            PlayButton btn = (PlayButton) baseView.findViewById(R.id.playBtn);
            btn.setLeftSide(comeMsg);
        }

        return baseView;
    }

    // TODO: 16/7/11 wtf, refactor to use HouseInfoMessageType
    public View createViewByType(int type, boolean comeMsg) {
        View baseView;
        switch (type) {
            case 2:
                baseView = inflater.inflate(comeMsg ? R.layout.chat_house_info_receive : R.layout.chat_house_info_send, null);
                break;
            default:
                throw new IllegalStateException();
        }
        return baseView;
    }

    private void setContentLayoutLength(int time) {
        ViewGroup.LayoutParams params = placeView.getLayoutParams();
        int length = 150 + time * 20;
        int max = (int) context.getResources().getDimension(R.dimen.chat_ContentMaxWidth);
        if (length > max) {
            params.width = max;
        } else {
            params.width = length;
        }
        placeView.setLayoutParams(params);
    }

    private enum MsgViewType {
        ComeText(0), ToText(1), ComeImage(2), ToImage(3), ComeAudio(4), ToAudio(5), ComeLocation(6), ToLocation(7), ChangeHouse(8);
        int value;

        MsgViewType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public interface ClickListener {
        void onFailButtonClick(AVIMTypedMessage msg);

        void onLocationViewClick(AVIMLocationMessage locMsg);

        void onImageViewClick(AVIMImageMessage imageMsg);

        void onAudioLongClick(AVIMAudioMessage audioMessage);
    }
}
