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
        type = 2
)
public class AVIMHouseMessage extends AVIMTypedMessage {
    @AVIMMessageField(
            name = "_lcattrs"
    )
    Map<String, Object> attrs;

    public static final Creator<AVIMHouseMessage> CREATOR = new AVIMMessageCreator(AVIMHouseMessage.class);

    public AVIMHouseMessage() {
        AVIMMessageManager.registerAVIMMessageType(this.getClass());
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
}
