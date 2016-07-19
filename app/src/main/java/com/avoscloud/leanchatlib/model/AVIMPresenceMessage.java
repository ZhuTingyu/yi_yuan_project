package com.avoscloud.leanchatlib.model;

import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

/**
 * Created by Alsor Zhou on 16/7/19.
 */
@AVIMMessageType(type = 5)
public class AVIMPresenceMessage extends AVIMTypedMessage {
    @AVIMMessageField(name = "op")
    String op;

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }
}
