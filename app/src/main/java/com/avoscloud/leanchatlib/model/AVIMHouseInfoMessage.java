package com.avoscloud.leanchatlib.model;

import com.avos.avoscloud.im.v2.AVIMMessageCreator;
import com.avos.avoscloud.im.v2.AVIMMessageField;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.AVIMMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by KevinLee on 2016/5/10.
 */
@AVIMMessageType(
        type = 2
)
public class AVIMHouseInfoMessage extends AVIMTypedMessage {

    @AVIMMessageField(name = "_lcattrs")
    JSONObject attrs;
    @AVIMMessageField(name = "_lctext")
    private String houseName;
    @AVIMMessageField(name = "_lctext")
    private String houseAddress;
    @AVIMMessageField(name = "_lctext")
    private String houseImage;

    public String getHouseName() {
        return houseName;
    }

    public void setHouseName(String houseName) {
        this.houseName = houseName;
    }

    public String getHouseAddress() {
        return houseAddress;
    }

    public void setHouseAddress(String houseAddress) {
        this.houseAddress = houseAddress;
    }

    public String getHouseImage() {
        return houseImage;
    }

    public void setHouseImage(String houseImage) {
        this.houseImage = houseImage;
    }
    public static final Creator<AVIMHouseInfoMessage> CREATOR = new AVIMMessageCreator(AVIMHouseInfoMessage.class);

    public AVIMHouseInfoMessage(){
        AVIMMessageManager.registerAVIMMessageType(this.getClass());
    }

//    public Map<String, Object> getAttrs() {
//        return attrs;
//    }
//
//    public void setAttrs(Map<String, Object> attrs) {
//        this.attrs = attrs;
//    }

    public JSONObject getAttrs() {
        return attrs;
    }

    public void setAttrs(JSONObject attrs) throws JSONException {
        this.attrs = attrs;
    }
}
