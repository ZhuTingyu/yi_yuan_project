package com.yuan.skeleton.activities;

import android.os.Bundle;

import com.yuan.skeleton.R;
import com.yuan.skeleton.ui.fragment.WebViewFragment;

/**
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
