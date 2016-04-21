package com.yuan.skeleton.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.yuan.skeleton.application.Injector;

import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

/**
 * Created by LiFengYi on 16/1/26.
 */

public class JsonParse {

    private static final JsonParse instance = new JsonParse();
    private static JSONObject jsonObject;

    @Inject
    SharedPreferences sp;

    private JsonParse(){
        Injector.inject(this);
    }

    public static JsonParse getInstance(){
        return instance;
    }

    private void initLoginJson(){
        String json = sp.getString("userLogin",null);
        if(TextUtils.isEmpty(json))
            return;

        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //TODO 获取登录类型
    public boolean judgeUserType() throws JSONException {
        initLoginJson();
        if(jsonObject !=null && "1".equals(String.valueOf(jsonObject.get("user_type"))))
            return true;
        else
            return false;
    }

}
