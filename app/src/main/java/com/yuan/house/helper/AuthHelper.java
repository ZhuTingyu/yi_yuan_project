package com.yuan.house.helper;

import android.content.SharedPreferences;

import com.dimo.utils.StringUtil;
import com.yuan.house.common.Constants;

import org.json.JSONException;

import java.util.HashMap;

import javax.inject.Inject;

/**
 * Created by Alsor Zhou on 16/6/10.
 */
public class AuthHelper {
    @Inject
    static SharedPreferences prefs;

    private static boolean userAlreadyLogin(String json) {
        HashMap<String, String> params = null;
        try {
            params = StringUtil.JSONString2HashMap(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (params.get("user_info") != null) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean userAlreadyLogin() {
        return userAlreadyLogin(userLoginInfomation());
    }

    public static String getUserId(String json) {
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            if (params.get("user_info") != null)
                params = StringUtil.JSONString2HashMap(params.get("user_info"));
            else
                params = StringUtil.JSONString2HashMap(params.get("agency_info"));

            return params.get("user_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String userId() {
        return getUserId(userLoginInfomation());
    }

    public static String userType() {
        // TODO: 16/6/10 user / agency
        String dummy = userLoginInfomation();
        return dummy;
    }
    private static String getToken(String json) {
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            return params.get("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
        String json = userLoginInfomation();

        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            return params.get("target_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String userLoginInfomation() {
        String json = prefs.getString(Constants.kWebDataKeyUserLogin, null);
        return json;
    }
}
