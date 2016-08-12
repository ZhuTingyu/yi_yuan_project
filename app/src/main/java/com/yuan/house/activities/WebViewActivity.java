package com.yuan.house.activities;

import android.os.Bundle;

import com.yuan.house.R;
import com.yuan.house.ui.fragment.WebViewFragment;

/**
 * General Activity for WebView Holder to open normal links
 * Created by yj on 2015/8/16.
 */
public class WebViewActivity extends WebViewBasedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebViewFragment fragment = new WebViewFragment();
        Bundle arguments = new Bundle();
        arguments.putString("url", mUrl);
        fragment.setArguments(arguments);
        mFragmentTransaction.replace(R.id.content_frame, fragment);
        mFragmentTransaction.commit();
    }
}
