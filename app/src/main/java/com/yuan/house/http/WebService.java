package com.yuan.house.http;

import android.content.SharedPreferences;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.helper.AuthHelper;

import org.apache.http.HttpEntity;

import java.util.HashMap;

import javax.inject.Inject;


/**
 * Created by Alsor Zhou on 10/16/15.
 */
public class WebService {

    private static WebService ourInstance = new WebService();

    @Inject
    SharedPreferences prefs;

    private WebService() {
        Injector.inject(this);
    }

    public static WebService getInstance() {
        return ourInstance;
    }

    public void postMultiPartFormDataFile(RequestParams requestParams, AsyncHttpResponseHandler responseHandler) {
        RestClient.getInstance().post(Constants.kWebServiceFileUpload, authTokenHeader(), requestParams, responseHandler);
    }

    public void postMultiPartFormImageFile(HttpEntity entity, AsyncHttpResponseHandler responseHandler) {
        RestClient.getInstance().post(Constants.kWebServiceImageUpload, authTokenHeader(), entity, responseHandler);
    }

    private HashMap<String, String> authTokenHeader() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken());

        return hashMap;
    }
}
