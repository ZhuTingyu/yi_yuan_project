package com.yuan.house.helper;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.yuan.house.common.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.inject.Inject;

/**
 * Created by Alsor Zhou on 16/6/10.
 */
public class AuthHelper {
    @Inject
    static SharedPreferences prefs;

    private static boolean userAlreadyLogin(String data) {
        if (TextUtils.isEmpty(data)) return false;
        
        JSONObject object = null;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.optString("user_info") != null;
    }

    public static boolean userAlreadyLogin() {
        return userAlreadyLogin(userLoginInfomation());
    }

    public static String getUserId(String data) {
        JSONObject object = null;
        JSONObject user = null;
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

    public static String userId() {
        return getUserId(userLoginInfomation());
    }

    private static UserType userType() {
        UserType type = UserType.USER;

        String raw = prefs.getString(Constants.kWebDataKeyLoginType, null);
        if ("agency".equals(raw)) {
            type = UserType.AGENCY;
        }
        return type;
    }

    public static boolean iAmUser() {
        if (userType() == UserType.USER) return true;

        return false;
    }

    private static String getToken(String data) {
        JSONObject object = null;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.optString("token");
    }

    public static String userToken() {
        return getToken(userLoginInfomation());
    }

    public static HashMap<String, String> authTokenJsonHeader() {
        String token = getToken(userLoginInfomation());

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(Constants.kHttpReqKeyAuthToken, token);
        hashMap.put(Constants.kHttpReqKeyContentType, "application/json");

        return hashMap;
    }

    public static String targetId() {
        String data = userLoginInfomation();

        JSONObject object = null;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.optString("target_id");
    }

    private static String userLoginInfomation() {
        String json = prefs.getString(Constants.kWebDataKeyUserLogin, null);

        return json;
    }

    enum UserType {
        USER(0), AGENCY(1);

        private int value;

        UserType(int value) {
            this.value = value;
        }
    }
}
