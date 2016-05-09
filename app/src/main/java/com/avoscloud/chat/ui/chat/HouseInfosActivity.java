package com.avoscloud.chat.ui.chat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ListView;

import com.dimo.utils.StringUtil;
import com.yuan.skeleton.R;
import com.yuan.skeleton.common.Constants;
import com.yuan.skeleton.utils.OkHttpClientManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;

import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Request;

/**
 * Created by KevinLee on 2016/5/9.
 */
public class HouseInfosActivity extends FragmentActivity {
    private ListView listView;
    private SharedPreferences prefs;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_house_info_layout);
        listView = (ListView) findViewById(R.id.listview);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String json = prefs.getString("userLogin",null);
        Log.i("json",json);
        String userId = getUserId(json);
        String token = getToken(json);
        OkHttpUtils.get().url(Constants.kWebServiceSwitchable + userId + "/4")
                .addHeader("Content-Type","application/json")
                .addHeader("token",token)
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onAfter() {
                        super.onAfter();
                        Log.i("onAfter","==================================================");
                    }

                    @Override
                    public void onBefore(Request request) {
                        super.onBefore(request);
                        Log.i("onBefore","==================================================");
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.i("onError",e.getMessage());
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.i("onResponse",response);
                    }
                });
    }

    private String getToken(String json){
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            return params.get("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUserId(String json){
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            if(params.get("user_info") != null)
                params = StringUtil.JSONString2HashMap(params.get("user_info"));
            else
                params = StringUtil.JSONString2HashMap(params.get("agency_info"));

            return params.get("user_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
