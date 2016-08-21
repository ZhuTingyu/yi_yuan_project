package com.avoscloud.leanchatlib.model;

import com.avos.avoscloud.im.v2.AVIMMessageCreator;
import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

import java.util.Map;

/**
 * Created by KevinLee on 2016/5/10.
 */
@AVIMMessageType(
        type = 3
)
public class AVIMCardMessage extends AVIMTypedMessage {
    @AVIMMessageField(
            name = "_lcattrs"
    )
    Map<String, Object> attrs;

    public static final Creator<AVIMCardMessage> CREATOR = new AVIMMessageCreator(AVIMCardMessage.class);

    public AVIMCardMessage() {
        AVIMMessageManager.registerAVIMMessageType(this.getClass());
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    public static void registerMessageType() {
        AVIMCardMessage temp = new AVIMCardMessage();
        AVIMMessageManager.registerAVIMMessageType(temp.getClass());
    }
}
