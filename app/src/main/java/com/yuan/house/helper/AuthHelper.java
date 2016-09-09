package com.yuan.house.helper;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.inject.Inject;

/**
 * Created by Alsor Zhou on 16/6/10.
 */
public class AuthHelper {
    private static AuthHelper instance = new AuthHelper();
    @Inject
    SharedPreferences prefs;
    private String userToken;
    private String userId;
    private String userLoginInfo;

    public JSONObject getUserLoginObject() {
        String info = prefs.getString(Constants.kWebDataKeyUserLogin, null);
        JSONObject object = null;
        try {
            object = new JSONObject(info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    private UserType userType;

    public AuthHelper() {
        Injector.inject(this);
    }

    public static AuthHelper getInstance() {
        return instance;
    }

    public String getUserLoginInfo() {
        return userLoginInfo;
    }

    private UserType getUserType(String data) {
        if (TextUtils.isEmpty(data)) return UserType.USER;

        UserType type = UserType.USER;

        JSONObject object;
        try {
            object = new JSONObject(data);

            if (object.optJSONObject("user_info") == null) {
                type = UserType.AGENCY;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return type;
    }

    public boolean iAmUser() {
        return userType == UserType.USER;
    }

    private String getToken(String data) {
        if (TextUtils.isEmpty(data)) return null;

        JSONObject object = null;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.optString("token");
    }

    public HashMap<String, String> authTokenJsonHeader() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(Constants.kHttpReqKeyAuthToken, getUserToken());
        hashMap.put(Constants.kHttpReqKeyContentType, "application/json");

        return hashMap;
    }

    public String getUserToken() {
        return userToken;
    }

    public String getUserId() {
        return userId;
    }

    private boolean userAlreadyLogin(String data) {
        if (TextUtils.isEmpty(data)) return false;

        JSONObject object = null;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.optString("user_info") != null;
    }

    public boolean userAlreadyLogin() {
        return userAlreadyLogin(userLoginInfo);
    }

    private String getUserId(String data) {
        if (TextUtils.isEmpty(data)) return null;

        JSONObject object = null;
        JSONObject user;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        user = object.optJSONObject("user_info");
        if (user == null) {
            user = object.optJSONObject("agency_info");
        }

        return user.optString("user_id");
    }

    public void evaluateUserLogin(String data) {
        userLoginInfo = data;

        userToken = getToken(data);
        userId = getUserId(data);
        userType = getUserType(data);
    }

    enum UserType {
        USER(0), AGENCY(1);

        private int value;

        UserType(int value) {
            this.value = value;
        }
    }
}
