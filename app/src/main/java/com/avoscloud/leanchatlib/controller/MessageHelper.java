package com.avoscloud.leanchatlib.controller;

import android.text.TextUtils;

import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.yuan.house.HouseMessageType;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;

import java.util.List;

/**
 * Created by lzw on 15/2/13.
 */
public class MessageHelper {
    public static String getFilePath(AVIMTypedMessage msg) {
        return com.avoscloud.leanchatlib.utils.PathUtils.getChatFilePath(msg.getMessageId());
    }

    public static boolean fromMe(AVIMTypedMessage msg) {
        ChatManager chatManager = ChatManager.getInstance();
        String selfId = chatManager.getSelfId();
        if (msg == null || TextUtils.isEmpty(selfId)) {
            return false;
        }

        return selfId.equals(msg.getFrom());
    }

    static String bracket(String s) {
        return String.format("[%s]", s);
    }

    public static CharSequence outlineOfMsg(AVIMTypedMessage msg) {
        HouseMessageType type = HouseMessageType.getMessageType(msg.getMessageType());
        switch (type) {
            case TextMessageType:
                return EmotionHelper.replace(DMApplication.getInstance(), ((AVIMTextMessage) msg).getText());
            case ImageMessageType:
                return bracket(DMApplication.getInstance().getString(R.string.chat_image));
            case LocationMessageType:
                AVIMLocationMessage locMsg = (AVIMLocationMessage) msg;
                String address = locMsg.getText();
                if (address == null) {
                    address = "";
                }
                return bracket(DMApplication.getInstance().getString(R.string.chat_position)) + address;
            case AudioMessageType:
                return bracket(DMApplication.getInstance().getString(R.string.chat_audio));
            case HouseMessageType:
                return bracket(DMApplication.getInstance().getString(R.string.chat_house));
            case CardMessageType:
                return bracket(DMApplication.getInstance().getString(R.string.chat_card));
        }
        return null;
    }

    public static String nameByUserIds(List<String> userIds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userIds.size(); i++) {
            String id = userIds.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(nameByUserId(id));
        }
        return sb.toString();
    }

    public static String nameByUserId(String id) {
        UserInfo user = ChatManager.getInstance().getUserInfoFactory().getUserInfoById(id);
        if (user != null) {
            return user.getUsername();
        } else {
            return id;
        }
    }
}
