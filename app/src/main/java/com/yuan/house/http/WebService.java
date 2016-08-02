package com.yuan.house.http;

import android.content.SharedPreferences;

import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.helper.AuthHelper;

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

    private HashMap<String, String> authTokenHeader() {
        HashMap<String, String> hashMap = new HashMap<>();

        hashMap.put(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken());

        return hashMap;
    }
}
