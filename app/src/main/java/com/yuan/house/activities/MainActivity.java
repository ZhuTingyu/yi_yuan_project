package com.yuan.house.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avos.avoscloud.PushService;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.yuan.house.BuildConfig;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.AuthEvent;
import com.yuan.house.event.LocationEvent;
import com.yuan.house.event.NetworkReachabilityEvent;
import com.yuan.house.event.PageEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.http.RestClient;
import com.yuan.house.ui.fragment.AgencyMainFragment;
import com.yuan.house.ui.fragment.AgencyMessageFragment;
import com.yuan.house.ui.fragment.CouponFragment;
import com.yuan.house.ui.fragment.LoginFragment;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.UserMainFragment;
import com.yuan.house.ui.fragment.UserMessageFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.ToastUtil;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/14.
 */

public class MainActivity extends WebViewBasedActivity implements WebViewFragment.OnFragmentInteractionListener {
    private final int kTabIndexOfCoupon = 0;
    private final int kTabIndexOfMain = 1;
    private final int kTabIndexOfMessage = 2;
    private final int kTabIndexOfProposal = 3;

    public LocationClient locationClient;
    public HouseLocationListener locationListener;
    private BottomNavigationBar bottomNavigationBar;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register event bus to receive events
        EventBus.getDefault().register(this);

        setContentView(R.layout.activity_main, false, true);

        Injector.inject(this);

        mContext = this;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dexter.checkPermissions(new MultiplePermissionsListener() {
                                        @Override
                                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                            token.continuePermissionRequest();
                                        }
                                    },
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        setupTabbarAppearance();

        // read user login credential from prefs
        AuthHelper.getInstance().evaluateUserLogin(prefs.getString(Constants.kWebDataKeyUserLogin, null));

        if (AuthHelper.getInstance().userAlreadyLogin()) {
            setupTabbarClickListener();
        }

        // Set default Activity when push comes
        PushService.setDefaultPushCallback(this, MainActivity.class);

        AVAnalytics.trackAppOpened(getIntent());

        initLocationClient();

        if (prefs.getString(Constants.kWebDataKeyUserLogin, null) != null) {
            switchToFragment(Constants.kFragmentTagMain);

            doAVUserLogin();            // 每次进入主界面都连接一下聊天服务器
        } else {
            switchToFragment(Constants.kFragmentTagLogin);
        }

        checkIfNotificationEnabledInSettings();

        executeAppVersionCheck();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            boolean dtm = bundle.getBoolean("dropToMessage");
            if (dtm) {
                bottomNavigationBar.selectTab(kTabIndexOfMessage);
            }
        }

//        new HostMonitorConfig(this)
//                .setBroadcastAction(BuildConfig.APPLICATION_ID + ".reachability")
//                .setCheckIntervalInSeconds(10)
//                .setSocketTimeoutInMilliseconds(1000)
//                .add(Constants.kWebServiceHost, 80)
//                .save();
    }

    private void checkIfNotificationEnabledInSettings() {
        if (!NotificationManagerCompat.from(mContext).areNotificationsEnabled()) {
            new AlertDialog.Builder(mContext)
                    .setTitle("提醒")
                    .setMessage("请打开应用的通知")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private void doAVUserLogin() {
        ChatManager.getInstance().avLogin();
    }

    public void onEvent(NetworkReachabilityEvent event) {
        if (event.getEventType() == NetworkReachabilityEvent.NetworkReachabilityEventEnum.OFFLINE) {
            ToastUtil.showShort(this, R.string.error_connection_failed);
        } else {
            ToastUtil.showShort(this, R.string.error_connection_okay);
        }
    }

    @Override
    public void onBackPressed() {
//        Fragment fragment = getVisibleFragment();

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, getString(R.string.msg_quit_app_press_twice_back), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public void onEvent(PageEvent event) {
        if (event.getEventType() == PageEvent.PageEventEnum.FINISHED) {

        } else if (event.getEventType() == PageEvent.PageEventEnum.FRIENDSHIP_UPDATE) {
            prefs.edit().putBoolean(Constants.kPrefsHasAgencyFriends, true).apply();

            bottomNavigationBar.selectTab(kTabIndexOfMain);
        } else if (event.getEventType() == PageEvent.PageEventEnum.DROP_TO_MESSAGE) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("dropToMessage", true);
            startActivity(intent);
        } else if (event.getEventType() == PageEvent.PageEventEnum.DROP_TO_CENTER) {
            bottomNavigationBar.selectTab(kTabIndexOfProposal);
        } else if (event.getEventType() == PageEvent.PageEventEnum.GET_LOCATION) {
            openMapActivity();
        }
    }

    public void onEvent(AuthEvent event) {
        if (event.getEventType() == AuthEvent.AuthEventEnum.LOGOUT) {
            // 注销已完成, 重新显示登录界面
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (event.getEventType() == AuthEvent.AuthEventEnum.NEED_LOGIN_AGAIN) {
            doAVUserLogin();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PushService.unsubscribe(this, "public");
        PushService.unsubscribe(this, "private");
        PushService.unsubscribe(this, "protected");

        AVInstallation.getCurrentInstallation().saveInBackground();

        EventBus.getDefault().unregister(this);
    }

    protected Fragment getFragment(String tag) {
        Fragment f = mFragmentManager.findFragmentByTag(tag);

        if (f != null && f.getClass() == ProposalFragment.class) {
            Timber.i("Found Fragment : " + tag);

            return f;
        }

        if (tag.equals(Constants.kFragmentTagCoupon)) {
            f = CouponFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagMain)) {
            if (AuthHelper.getInstance().iAmUser()) {
                f = UserMainFragment.newInstance();
            } else {
                f = AgencyMainFragment.newInstance();
            }
        } else if (tag.equals(Constants.kFragmentTagMessage)) {
            if (AuthHelper.getInstance().iAmUser()) {
                f = UserMessageFragment.newInstance();
            } else {
                f = AgencyMessageFragment.newInstance();
            }
        } else if (tag.equals(Constants.kFragmentTagLogin)) {
            f = LoginFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagProposal)) {
            f = ProposalFragment.newInstance();
        } else {
            f = WebViewFragment.newInstance();
        }

        Timber.v("NOT Found Fragment : " + tag + ", need to create!!!");

        return f;
    }

    public BottomNavigationBar getBottomNavigationBar() {
        return bottomNavigationBar;
    }

    public void setupTabbarAppearance() {
        bottomNavigationBar = ButterKnife.findById(getTabBar(), R.id.bottom_navigation_bar);
        bottomNavigationBar.setMode(BottomNavigationBar.MODE_FIXED);
        bottomNavigationBar
                .addItem(new BottomNavigationItem(R.drawable.ic_ticket, "优惠券")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_home, "房源")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_chat, "消息")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_suggest, "建议")).setActiveColor(R.color.primary_color_scheme)
                .setFirstSelectedPosition(kTabIndexOfMain)
                .initialise();
    }

    private void setupTabbarClickListener() {

        bottomNavigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                if (!DMApplication.getInstance().isAllowUserToUseFullFeatureVersion()) {
                    Utils.alertDialog(MainActivity.this, "请先领券优惠券");

                    return;
                }

                switch (position) {
                    case kTabIndexOfCoupon:
                        if (BuildConfig.kDebugCouponFeature) {
                            switchToFragment(Constants.kFragmentTagCoupon);
                        }
                        break;
                    case kTabIndexOfMain:
                        switchToFragment(Constants.kFragmentTagMain);
                        break;
                    case kTabIndexOfMessage:
                        switchToFragment(Constants.kFragmentTagMessage);
                        break;
                    case kTabIndexOfProposal:
                        switchToFragment(Constants.kFragmentTagProposal);
                        break;
                }
            }

            @Override
            public void onTabUnselected(int position) {
            }

            @Override
            public void onTabReselected(int position) {
                switch (position) {
                    case kTabIndexOfMain:
                        switchToFragment(Constants.kFragmentTagMain);
                        break;
                    case kTabIndexOfMessage:
                        switchToFragment(Constants.kFragmentTagMessage);
                        break;
                }
            }
        });
    }

    /**
     * Callback when user has sign in
     *
     * @param data user credentials
     */
    public void onBridgeSignIn(final String data) {
        JSONObject object = null;
        try {
            object = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String key = object.optString("key");
        String value = object.optString("value");

        SharedPreferences.Editor editor = prefs.edit();
        if (value == null || value.equals("null")) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.apply();

        if (Constants.kWebDataKeyUserLogin.equals(key)) {
            try {
                AuthHelper.getInstance().evaluateUserLogin(value);

                JSONObject holder = new JSONObject(value);
                JSONObject user;

                // 订阅频道，当该频道消息到来的时候，打开对应的 Activity
                PushService.subscribe(this, "public", MainActivity.class);
                PushService.subscribe(this, "private", MainActivity.class);
                PushService.subscribe(this, "protected", MainActivity.class);

                // save `user_id' or `agency_id' in AVInstallation
                AVInstallation installation = AVInstallation.getCurrentInstallation();
                user = holder.optJSONObject("user_info");
                if (user == null) {
                    user = holder.optJSONObject("agency_info");
                    installation.put("agency_id", user.optString("user_id"));
                } else {
                    installation.put("user_id", user.optString("user_id"));
                }

                installation.saveInBackground();
                Timber.v("Installation id: " + installation.getInstallationId());

                prefs.edit().putString("AVInstallationId", installation.getInstallationId()).apply();

                String userName = user.optString("lean_user");
                String passwd = user.optString("lean_passwd");

                avUserLogin(userName, passwd);

                setupTabbarClickListener();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void avUserLogin(final String username, String password) {
        AVUser.logInInBackground(username, password,
                new LogInCallback<AVUser>() {
                    @Override
                    public void done(AVUser avUser, AVException e) {
                        if (e != null) {
                            // com.avos.avoscloud.AVException: javax.net.ssl.SSLHandshakeException:
                            //      com.android.org.bouncycastle.jce.exception.ExtCertPathValidatorException:
                            //      Could not validate certificate: Certificate not valid until Wed Nov
                            //      06 05:36:50 GMT+08:00 2013 (compared to Wed Jul 27 12:00:57 GMT+08:00 2011)
                            e.printStackTrace();
                            ToastUtil.showShort(mContext, R.string.error_leancloud_login_failed);
                        } else if (avUser != null) {
                            String chatUserId = avUser.getObjectId();
                            prefs.edit()
                                    .putString("avUserLogin", username)
                                    .putString(Constants.kLeanChatCurrentUserObjectId, chatUserId)
                                    .apply();

                            doAVUserLogin();

                            switchToFragment(Constants.kFragmentTagMain);

                            getBottomNavigationBar().clearAll();
                            setupTabbarAppearance();
                        }
                    }
                });
    }

    private void initLocationClient() {
        locationClient = new LocationClient(mContext);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(5000);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        locationClient.setLocOption(option);

        locationListener = new HouseLocationListener();
        locationClient.registerLocationListener(locationListener);
        locationClient.start();
    }

    protected void executeAppVersionCheck() {
        boolean hasNewVersion = prefs.getString(Constants.kAppHasNewVersion, "0").equals("1");

        if (hasNewVersion) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.hint_found_new_version);
            builder.setMessage(R.string.hint_found_new_version_description);

            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadFileAndPrepareInstallation();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            if (!isFinishing()) {
                builder.create().show();
            }
        }
    }

    private void downloadFileAndPrepareInstallation() {
        String url = prefs.getString(Constants.kNewAppDownloadUrl, null);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(mContext, R.string.error_upgrade_app, Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle(R.string.txt_downloading);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        if (!isFinishing()) {
            progressDialog.show();
        }

        RestClient.getInstance().get(url, null, new FileAsyncHttpResponseHandler(getApplicationContext()) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Timber.e(throwable.toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                file.setReadable(true, false);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                mContext.startActivity(intent);
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                double por = (bytesWritten * 1.0 / totalSize) * 100;
                progressDialog.setProgress((int) por);
                if (bytesWritten == totalSize) {
                    progressDialog.dismiss();
                }
                super.onProgress(bytesWritten, totalSize);
            }
        });

    }

    public class HouseLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            DMApplication.getInstance().setLastActivatedLocation(location);

            // tell subscriber to update location
            EventBus.getDefault().post(new LocationEvent(LocationEvent.LocationEventEnum.UPDATED, location));

            locationClient.stop();
        }
    }
}
