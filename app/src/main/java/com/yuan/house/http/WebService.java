package com.yuan.house.http;

import android.content.SharedPreferences;

import com.dimo.http.RestClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.inject.Inject;


/**
 * Created by Alsor Zhou on 10/16/15.
 */
public class WebService {
    public static String kHttpReqKeyToken = "token";

    private static WebService ourInstance = new WebService();

    @Inject
    SharedPreferences prefs;

    private String kHttpReqKeyContentType = "Content-Type";

    private WebService() {
        Injector.inject(this);
    }

    public static WebService getInstance() {
        return ourInstance;
    }

    public void postMultiPartFormDataFile(RequestParams requestParams, AsyncHttpResponseHandler responseHandler) {
        RestClient.getInstance().post(Constants.kWebServiceFileUpload, authTokenHeader(), requestParams, responseHandler);
    }

    private String getAuthToken() {
        String token = null;
        String json = prefs.getString(Constants.kWebDataKeyUserLogin, null);
        try {
            JSONObject object = new JSONObject(json);
            token = object.optString(kHttpReqKeyToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return token;
    }

    private HashMap<String, String> authTokenHeader() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put(kHttpReqKeyToken, getAuthToken());

        return hashMap;
    }


    private HashMap<String, String> authTokenJsonHeader() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(kHttpReqKeyContentType, "application/json");
        return hashMap;
    }
}
