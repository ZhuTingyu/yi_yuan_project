package com.yuan.skeleton.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.skeleton.R;
import com.yuan.skeleton.ui.fragment.WebViewFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by yj on 2015/8/16.
 */
public class WebViewActivity extends WebViewBasedActivity {
    public static WebViewActivity instance;
    private Context mContext;
    private static final int REQUEST_MAP_CODE = 0XFF01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        mContext = this;
        WebViewFragment fragment = new WebViewFragment();
        Bundle arguments = new Bundle();
        arguments.putString("url", mUrl);
        fragment.setArguments(arguments);
        mFragmentTransaction.replace(R.id.content_frame, fragment);
        mFragmentTransaction.commit();
    }

    @Override
    protected void registerHandle() {
        super.registerHandle();
        bridge.registerHandler("selectMapLocation",new WebViewJavascriptBridge.WVJBHandler(){

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Intent intent = new Intent(mContext, MapActivity.class);
                startActivityForResult(intent,REQUEST_MAP_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_MAP_CODE && resultCode == Activity.RESULT_OK){
            //获取地图返回的地理位置
            String mapJson = data.getStringExtra("mapJson");
            try {
                bridge.callHandler("selectedMapLocation",new JSONObject(mapJson));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
