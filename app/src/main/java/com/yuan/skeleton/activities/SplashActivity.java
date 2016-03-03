package com.yuan.skeleton.activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

import com.yuan.skeleton.R;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.common.Constants;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.umeng.update.UmengUpdateAgent;

import org.json.JSONObject;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class SplashActivity extends FragmentActivity implements Handler.Callback {

    @Inject
    public SharedPreferences prefs;

    @InjectView(R.id.splash_image_view)
    ImageView imageView;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.splash_view);

        ButterKnife.inject(this);
        Injector.inject(this);

        updateSplashImage();

        UmengUpdateAgent.update(this);
    }

    private void updateSplashImage() {
        Callback callback = new Callback() {
            @Override
            public void onSuccess() {
                Timber.e("SUCCESS to load the splash picture");
                enterMainScreen();
            }

            @Override
            public void onError() {
                // what should do if show splash not success
                Timber.e("FAILED to load the splash picture");
                enterMainScreen();
            }
        };

        Picasso.with(this).load(R.mipmap.ic_launcher).into(imageView, callback);
    }

    private void enterMainScreen() {
        mHandler = new Handler(SplashActivity.this);

        JSONObject userLogin = null;
        try {
            userLogin = new JSONObject(prefs.getString("userLogin", null));
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Class nextClass;
//        if (userLogin == null) {
//            nextClass = SignInActivity.class;
//        } else {
            nextClass = MainActivity.class;
//        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), nextClass);
                startActivity(intent);

                SplashActivity.this.finish();
            }
        }, Constants.kSplashTimeInterval * 100);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
