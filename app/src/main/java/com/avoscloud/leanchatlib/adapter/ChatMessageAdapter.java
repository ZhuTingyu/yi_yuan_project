package com.avoscloud.leanchatlib.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import com.avoscloud.leanchatlib.model.AVIMHouseInfoMessage;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.avoscloud.leanchatlib.utils.PhotoUtils;
import com.avoscloud.leanchatlib.view.PlayButton;
import com.avoscloud.leanchatlib.view.ViewHolder;
import com.lfy.bean.Message;
import com.lfy.dao.DaoMaster;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.picasso.Picasso;
import com.yuan.house.R;

import org.ocpsoft.prettytime.PrettyTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


public class ChatMessageAdapter extends BaseListAdapter<AVIMTypedMessage> {
    private static PrettyTime prettyTime = new PrettyTime();
    private ConversationType conversationType;
    private int msgViewTypes = 9;
    private ClickListener clickListener;
    private Context context;
    private SharedPreferences prefs;
    private DaoMaster master;

    public ChatMessageAdapter(Context context, ConversationType conversationType) {
        super(context);
        this.context = context;
        this.conversationType = conversationType;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "chat-db", null);

        SQLiteDatabase db = helper.getWritableDatabase();
        this.master = new DaoMaster(db);
    }

    // time
    public static String millisecsToDateString(long timestamp) {
        long gap = System.currentTimeMillis() - timestamp;
        if (gap < 1000 * 60 * 60 * 24) {
            String s = prettyTime.format(new Date(timestamp));
            //return s.replace(" ", "");
            return s;
        } else {
            SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
            return format.format(new Date(timestamp));
        }
    }

    public static boolean haveTimeGap(long lastTime, long time) {
        int gap = 1000 * 60 * 3;
        return time - lastTime > gap;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public int getItemViewType(int position) {
        AVIMTypedMessage msg = datas.get(position);
        boolean comeMsg = messageSentByMe(msg);

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

    boolean messageSentByMe(AVIMTypedMessage msg) {
        return !MessageHelper.fromMe(msg);
    }

    public View getView(int position, View conView, ViewGroup parent) {
        AVIMMessage msg = datas.get(position);

        if (conView == null) {
            Message bean = new Message();
            bean.setDate(String.valueOf(msg.getTimestamp()));
            bean.setLeanId(prefs.getString("leanId", "0"));
            bean.setAuditType(prefs.getString("auditType", "0"));
            bean.setHouseId(prefs.getString("houseId", "0"));
            bean.setIs_read(true);

            if (msg instanceof AVIMHouseInfoMessage) {
                AVIMHouseInfoMessage houseInfoMessage = (AVIMHouseInfoMessage) msg;

                boolean isComMsg = messageSentByMe(houseInfoMessage);
                conView = createViewByType(houseInfoMessage.getMessageType(), isComMsg);

                initHouseMessageView(conView, houseInfoMessage, isComMsg);

                bean.setMessage("[房源信息]");
            } else if (msg instanceof AVIMTypedMessage) {
                AVIMTypedMessage typedMessage = (AVIMTypedMessage) msg;

                boolean isComMsg = messageSentByMe(typedMessage);
                conView = createViewByType(AVIMReservedMessageType.getAVIMReservedMessageType(typedMessage.getMessageType()), isComMsg);

                initReservedMessageView(conView, position, typedMessage, isComMsg, bean);
            }

            // FIXME: 16/6/22 wtf insert database record in UI layer?
//            if (position == datas.size() - 1) {
//                daoSession.getMessageDao().insertOrReplace(bean);
//            }
        }

        return conView;
    }

    private void initHouseMessageView(View conView, AVIMTypedMessage msg, boolean isMessageSentByMe) {
        AVIMHouseInfoMessage message = (AVIMHouseInfoMessage) msg;

        Map<String, Object> map = message.getAttrs();
        JSONObject object = (JSONObject) JSON.toJSON(map);

        if (object.size() == 0) return;

        ImageView img = ViewHolder.findViewById(conView, R.id.img);
        TextView title = ViewHolder.findViewById(conView, R.id.title);
        TextView area = ViewHolder.findViewById(conView, R.id.area);
        TextView house_params = ViewHolder.findViewById(conView, R.id.house_params);

        View statusSendFailed = ViewHolder.findViewById(conView, R.id.status_send_failed);
        View statusSendSucceed = ViewHolder.findViewById(conView, R.id.status_send_succeed);
        View statusSendStart = ViewHolder.findViewById(conView, R.id.status_send_start);

        JSONArray images = object.getJSONArray("images");

        if (images != null) {
            Picasso.with(ctx).load(images.getString(0)).into(img);
        }

        title.setText(object.getString("estate_name"));
        area.setText(object.getString("acreage") + "㎡");

        StringBuilder sb = new StringBuilder();
        sb.append(object.getString("room_count"));
        sb.append("室");
        sb.append(object.getString("parlour_count"));
        sb.append("厅");
        house_params.setText(sb.toString());

        if (isMessageSentByMe == false) {
            hideStatusViews(statusSendStart, statusSendFailed, statusSendSucceed);
            setSendFailedBtnListener(statusSendFailed, msg);
            switch (msg.getMessageStatus()) {
                case AVIMMessageStatusFailed:
                    statusSendFailed.setVisibility(View.VISIBLE);
                    break;
                case AVIMMessageStatusSent:
                    if (conversationType == ConversationType.Single) {
                        statusSendSucceed.setVisibility(View.VISIBLE);
                    }
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

    //初始化AVIMReservedMessageType的视图
    private void initReservedMessageView(View conView, int position, AVIMTypedMessage msg, boolean isComMsg, Message message) {
        TextView sendTimeView = ViewHolder.findViewById(conView, R.id.sendTimeView);
        TextView contentView = ViewHolder.findViewById(conView, R.id.textContent);
        View contentLayout = ViewHolder.findViewById(conView, R.id.contentLayout);
        ImageView imageView = ViewHolder.findViewById(conView, R.id.imageView);
        ImageView avatarView = ViewHolder.findViewById(conView, R.id.avatar);
        PlayButton playBtn = ViewHolder.findViewById(conView, R.id.playBtn);
        TextView timeAudio = ViewHolder.findViewById(conView, R.id.time);
        TextView locationView = ViewHolder.findViewById(conView, R.id.locationView);
        TextView usernameView = ViewHolder.findViewById(conView, R.id.username);

        View statusSendFailed = ViewHolder.findViewById(conView, R.id.status_send_failed);
        View statusSendSucceed = ViewHolder.findViewById(conView, R.id.status_send_succeed);
        View statusSendStart = ViewHolder.findViewById(conView, R.id.status_send_start);

        // timestamp
        if (position == 0 || haveTimeGap(datas.get(position - 1).getTimestamp(),
                msg.getTimestamp())) {
            sendTimeView.setVisibility(View.VISIBLE);
            sendTimeView.setText(millisecsToDateString(msg.getTimestamp()));
        } else {
            sendTimeView.setVisibility(View.GONE);
        }

        UserInfo user = ChatManager.getInstance().getUserInfoFactory().getUserInfoById(msg.getFrom());
        if (user == null) {
            throw new NullPointerException("user is null");
        }
        if (isComMsg) {
            if (conversationType == null) {
                throw new NullPointerException("conv type is null");
            }
            if (conversationType == ConversationType.Single) {
                usernameView.setVisibility(View.GONE);
            } else {
                usernameView.setVisibility(View.VISIBLE);
                usernameView.setText(user.getUsername());
            }
        }
        ImageLoader.getInstance().displayImage(user.getAvatarUrl(), avatarView, PhotoUtils.avatarImageOptions);

        AVIMReservedMessageType type = AVIMReservedMessageType.getAVIMReservedMessageType(msg.getMessageType());
        switch (type) {
            case TextMessageType:
                AVIMTextMessage textMsg = (AVIMTextMessage) msg;
                contentView.setText(EmotionHelper.replace(ChatManager.getContext(), textMsg.getText()));
                contentLayout.requestLayout();
                message.setMessage(textMsg.getText());
                break;
            case ImageMessageType:
                AVIMImageMessage imageMsg = (AVIMImageMessage) msg;
                PhotoUtils.displayImageCacheElseNetwork(imageView, MessageHelper.getFilePath(imageMsg),
                        imageMsg.getFileUrl());
                setImageOnClickListener(imageView, imageMsg);
                message.setMessage("[图片]");
                break;
            case AudioMessageType:
                AVIMAudioMessage audioMessage = (AVIMAudioMessage) msg;
                initPlayBtn(msg, playBtn, audioMessage, timeAudio);
                message.setMessage("[语音]");
                break;
            case LocationMessageType:
                setLocationView(msg, locationView);
                message.setMessage("[位置]");
                break;
            default:
                break;
        }
        if (isComMsg == false) {
            hideStatusViews(statusSendStart, statusSendFailed, statusSendSucceed);
            setSendFailedBtnListener(statusSendFailed, msg);
            switch (msg.getMessageStatus()) {
                case AVIMMessageStatusFailed:
                    statusSendFailed.setVisibility(View.VISIBLE);
                    break;
                case AVIMMessageStatusSent:
                    if (conversationType == ConversationType.Single) {
                        statusSendSucceed.setVisibility(View.VISIBLE);
                    }
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

    private void hideStatusViews(View statusSendStart, View statusSendFailed, View statusSendSucceed) {
        statusSendFailed.setVisibility(View.GONE);
        statusSendStart.setVisibility(View.GONE);
        statusSendSucceed.setVisibility(View.GONE);
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

    private void initPlayBtn(AVIMTypedMessage msg, PlayButton playBtn, AVIMAudioMessage audioMessage, TextView timeAudio) {
        playBtn.setLeftSide(messageSentByMe(msg));
        AudioHelper audioHelper = AudioHelper.getInstance();
        playBtn.setAudioHelper(audioHelper);
        playBtn.setPath(MessageHelper.getFilePath(msg));
        Object obj = audioMessage.getFileMetaData().get("duration");
        timeAudio.setText(String.valueOf((int) Double.parseDouble(obj.toString())));
        setItemOnLongClickListener(playBtn, audioMessage);
    }

    private void setItemOnLongClickListener(PlayButton playBtn, final AVIMAudioMessage audioMessage) {
        playBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (clickListener != null) {
                    clickListener.onAudioLongClick(audioMessage);
                }
                return false;
            }
        });
    }

    private void setImageOnClickListener(ImageView imageView, final AVIMImageMessage imageMsg) {
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onImageViewClick(imageMsg);
                }
            }
        });
    }

    public View createViewByType(AVIMReservedMessageType type, boolean comeMsg) {
        View baseView;
        if (comeMsg) {
            baseView = inflater.inflate(R.layout.chat_item_base_left, null);
        } else {
            baseView = inflater.inflate(R.layout.chat_item_base_right, null);
        }
        LinearLayout contentView = (LinearLayout) baseView.findViewById(R.id.contentLayout);
        int contentId;
        switch (type) {
            case TextMessageType:
                contentId = R.layout.chat_item_text;
                break;
            case AudioMessageType:
                contentId = R.layout.chat_item_audio;
                break;
            case ImageMessageType:
                contentId = R.layout.chat_item_image;
                break;
            case LocationMessageType:
                contentId = R.layout.chat_item_location;
                break;
            default:
                throw new IllegalStateException();
        }
        contentView.removeAllViews();
        View content = inflater.inflate(contentId, null, false);
        if (type == AVIMReservedMessageType.AudioMessageType) {
            PlayButton btn = (PlayButton) content.findViewById(R.id.playBtn);
            btn.setLeftSide(comeMsg);
        } else if (type == AVIMReservedMessageType.TextMessageType) {
            TextView textView = (TextView) content;
            if (comeMsg) {
                textView.setTextColor(Color.BLACK);
            } else {
                textView.setTextColor(Color.WHITE);
            }
        }
        contentView.addView(content);
        return baseView;
    }

    public View createViewByType(int type, boolean comeMsg) {
        View baseView;
        if (comeMsg) {
            baseView = inflater.inflate(R.layout.chat_item_base_left, null);
        } else {
            baseView = inflater.inflate(R.layout.chat_item_base_right, null);
        }
        LinearLayout contentView = (LinearLayout) baseView.findViewById(R.id.contentLayout);
        int contentId;
        switch (type) {
            case 2:
                contentId = R.layout.chat_house_info_adapter;
                break;
            default:
                throw new IllegalStateException();
        }
        contentView.removeAllViews();
        View content = inflater.inflate(contentId, null, false);
        contentView.addView(content);
        return baseView;
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
