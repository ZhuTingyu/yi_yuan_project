package com.yuan.house.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;

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
    }

    @OnClick(R.id.btn_dummy_ip_setting)
    public void onConfirm(View view) {
        String input;
        String inputRaw = editText.getText().toString();

        if (TextUtils.isEmpty(inputRaw)) {
            if (!TextUtils.isEmpty(ip)) {
                DMApplication.getInstance().setHtmlExtractedFolder(ip);
                DMApplication.getInstance().setRootPagesFolder(ip + "/html/pages");
            }
        } else {
            input = "http://" + inputRaw;
            DMApplication.getInstance().setHtmlExtractedFolder(input);
            DMApplication.getInstance().setRootPagesFolder(input + "/html/pages");
            preferences.edit().putString("ip_settings", inputRaw).commit();
        }

        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
        finish();
    }
}
