package com.yuan.house.activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.yuan.house.HeartbeatService;
import com.yuan.house.R;
import com.yuan.house.application.Injector;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class SplashActivity extends FragmentActivity implements Handler.Callback {

    @Inject
    public SharedPreferences prefs;

    @BindView(R.id.splash_image_view)
    ImageView imageView;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.splash_view);

        Injector.inject(this);
        ButterKnife.bind(this);

        updateSplashImage();

        startService(new Intent(getBaseContext(), HeartbeatService.class));
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
//        mHandler = new Handler(this);
//
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);

        finish();
//            }
//        }, Constants.kSplashTimeInterval * 100);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
