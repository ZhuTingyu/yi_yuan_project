package com.yuan.skeleton.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.yuan.skeleton.R;
import com.yuan.skeleton.application.DMApplication;
import com.yuan.skeleton.application.Injector;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by Alsor Zhou on 10/13/15.
 */
public class IPSettingActivity extends Activity {
    @Inject
    SharedPreferences preferences;

    @InjectView(R.id.et_dummy_ip_setting)
    EditText editText;

    String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ip_setting);

        Injector.inject(this);
        ButterKnife.inject(this);

        ip = preferences.getString("ip_settings", "");
        if (!TextUtils.isEmpty(ip)) {
            editText.setText(ip);
        }
    }

    @OnClick(R.id.btn_dummy_ip_setting)
    public void onConfirm(View view) {
        String input;
        String inputRaw = editText.getText().toString();

        if (TextUtils.isEmpty(inputRaw)) {
            if (!TextUtils.isEmpty(ip)) {
                DMApplication.getInstance().setHtmlExtractedFolder(ip + "/pages");
                DMApplication.getInstance().setRootPagesFolder(ip + "/pages");
            }
        } else {
            input = "http://" + inputRaw;
            DMApplication.getInstance().setHtmlExtractedFolder(input + "/html/pages");
            DMApplication.getInstance().setRootPagesFolder(input + "/html/pages");
            preferences.edit().putString("ip_settings", inputRaw).commit();
        }

        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        finish();
    }
}
