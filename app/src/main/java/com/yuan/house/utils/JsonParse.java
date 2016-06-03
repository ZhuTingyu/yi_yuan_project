package com.yuan.house.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.yuan.house.application.Injector;

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
        String json = sp.getString("userType",null);
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
//        initLoginJson();
        if("user".equals(sp.getString("LoginType",null)))
            return true;
        else
            return false;
    }

}
