package com.yuan.house.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.R;

/**
 * Created by yj on 2015/8/16.
 */
public class WebViewActivity extends WebViewBasedActivity {
    private static final int REQUEST_MAP_CODE = 0XFF01;
    public static WebViewActivity instance;
    private Context mContext;

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MAP_CODE && resultCode == Activity.RESULT_OK) {
            //获取地图返回的地理位置
//            String mapJson = data.getStringExtra(Constant.kActivityParamFinishSelectLocationOnMap);
//            try {
//                getWebViewFragment().getBridge().callHandler("selectedMapLocation", new JSONObject(mapJson));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
