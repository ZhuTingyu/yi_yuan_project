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
public class AVIMNoticeWithHouseIdMessage extends AVIMTypedMessage {

    @AVIMMessageField(name = "_lcattrs")
    Map<String, Object> attrs;
    @AVIMMessageField(name = "_lctext")
    private String houseId;

    public String getHouseId() {
        return houseId;
    }

    public void setHouseId(String houseId) {
        this.houseId = houseId;
    }
    public static final Creator<AVIMNoticeWithHouseIdMessage> CREATOR = new AVIMMessageCreator(AVIMNoticeWithHouseIdMessage.class);

    public AVIMNoticeWithHouseIdMessage(){
        AVIMMessageManager.registerAVIMMessageType(this.getClass());
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
}
