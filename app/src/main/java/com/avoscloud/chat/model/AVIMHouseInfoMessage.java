package com.avoscloud.chat.model;

import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

import java.util.Map;

/**
 * Created by KevinLee on 2016/5/10.
 */
@AVIMMessageType(type = 123)
public class AVIMHouseInfoMessage extends AVIMTypedMessage {

    @AVIMMessageField(name = "_lcattrs")
    Map<String, Object> attrs;

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
}
