package com.yuan.house.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVGeoPoint;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avos.avoscloud.PushService;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.EmptyMultiplePermissionsListener;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.AuthEvent;
import com.yuan.house.event.LocationEvent;
import com.yuan.house.event.PageEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.ui.fragment.AgencyMainFragment;
import com.yuan.house.ui.fragment.AgencyMessageFragment;
import com.yuan.house.ui.fragment.LoginFragment;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.UserMainFragment;
import com.yuan.house.ui.fragment.UserMessageFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/14.
 */

public class MainActivity extends WebViewBasedActivity implements WebViewFragment.OnFragmentInteractionListener {
    public LocationClient locClient;
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
            Dexter.checkPermissions(new EmptyMultiplePermissionsListener(),
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        }

        setupTabbarAppearance();

        // read user login credential from prefs
        AuthHelper.getInstance().evaluateUserLogin(prefs.getString(Constants.kWebDataKeyUserLogin, null));
        AuthHelper.getInstance().evaluateUserType(prefs.getString(Constants.kWebDataKeyLoginType, null));

        if (AuthHelper.getInstance().userAlreadyLogin()) {
            setupTabbarClickListener();
        }

        // Set default Activity when push comes
        PushService.setDefaultPushCallback(this, MainActivity.class);

        // 订阅频道，当该频道消息到来的时候，打开对应的 Activity
        PushService.subscribe(this, "public", MainActivity.class);
        PushService.subscribe(this, "private", MainActivity.class);
        PushService.subscribe(this, "protected", MainActivity.class);

        AVAnalytics.trackAppOpened(getIntent());

        initBaiduLocClient();

        if (prefs.getString(Constants.kWebDataKeyUserLogin, null) != null) {
            switchToFragment(Constants.kFragmentTagMain);

// configure chat service
// 每次进入主界面都连接一下聊天服务器
//            if (AVUser.getCurrentUser() != null) {
            doAVUserLogin();
//            }
        } else {
            switchToFragment(Constants.kFragmentTagLogin);
        }

        checkIfNotificationEnabledInSettings();

        executeAppVersionCheck();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            boolean dtm = bundle.getBoolean("dropToMessage");
            if (dtm) {
                bottomNavigationBar.selectTab(1);
            }
        }
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
        ChatManager chatManager = ChatManager.getInstance();
        chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
        chatManager.openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);
        CacheService.registerUser(AVUser.getCurrentUser());
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
            bottomNavigationBar.selectTab(0);
        } else if (event.getEventType() == PageEvent.PageEventEnum.DROP_TO_MESSAGE) {
            // TODO: 16/7/30 kill other child pages
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("dropToMessage", true);
            startActivity(intent);
        }
    }

    public void onEvent(AuthEvent event) {
        if (event.getEventType() == AuthEvent.AuthEventEnum.LOGOUT) {
            // 注销已完成, 重新显示登录界面
            if (getWebViewFragment().getClass() != LoginFragment.class) {
                // API 11 +
                recreate();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.v("onDestroy");

        PushService.unsubscribe(this, "public");

        //退订之后需要重新保存 Installation
        AVInstallation.getCurrentInstallation().saveInBackground();
    }

    protected Fragment getFragment(String tag) {
// FIXME: 16/7/30 每次都重新刷新页面, 不使用之前创建的页面
        Fragment f = mFragmentManager.findFragmentByTag(tag);

        if (f != null && f.getClass() == ProposalFragment.class) {
            Timber.i("Found Fragment : " + tag);

            return f;
        }

        if (tag.equals(Constants.kFragmentTagMain)) {
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
        this.bottomNavigationBar = ButterKnife.findById(getTabBar(), R.id.bottom_navigation_bar);
        bottomNavigationBar
                .addItem(new BottomNavigationItem(R.drawable.ic_home, "房源")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_chat, "消息")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_suggest, "建议")).setActiveColor(R.color.primary_color_scheme)
                .setFirstSelectedPosition(0)
                .initialise();
    }

    private void setupTabbarClickListener() {
        bottomNavigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {
                switch (position) {
                    case 0:
                        switchToFragment(Constants.kFragmentTagMain);
                        break;
                    case 1:
                        switchToFragment(Constants.kFragmentTagMessage);
                        break;
                    case 2:
                        switchToFragment(Constants.kFragmentTagProposal);
                        break;
                }
            }

            @Override
            public void onTabUnselected(int position) {
            }

            @Override
            public void onTabReselected(int position) {
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
        editor.commit();

        if (Constants.kWebDataKeyUserLogin.equals(key)) {
            try {
                AuthHelper.getInstance().evaluateUserLogin(value);

                JSONObject holder = new JSONObject(value);
                JSONObject user;

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

                String userName = user.optString("lean_user");
                String passwd = user.optString("lean_passwd");

                avUserLogin(userName, passwd);

                setupTabbarClickListener();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (Constants.kWebDataKeyLoginType.equals(key)) {
            AuthHelper.getInstance().evaluateUserType(value);
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
                            ToastUtil.showShort(mContext, "消息服务器登陆失败, 请检查手机的时间设置是否正确");
                        } else if (avUser != null) {
                            String chatUserId = avUser.getObjectId();
                            prefs.edit()
                                    .putString("avUserLogin", username)
                                    .putString(Constants.kLeanChatCurrentUserObjectId, chatUserId)
                                    .commit();

                            doAVUserLogin();

                            switchToFragment(Constants.kFragmentTagMain);

                            getBottomNavigationBar().clearAll();
                            setupTabbarAppearance();
                        }
                    }
                });
    }

    private void initBaiduLocClient() {
        locClient = new LocationClient(mContext);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(5000);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        locClient.setLocOption(option);

        locationListener = new HouseLocationListener();
        locClient.registerLocationListener(locationListener);
        locClient.start();
    }

    public class HouseLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            int locType = location.getLocType();

            Timber.v("onReceiveLocation latitude=" + latitude +
                    " longitude=" + longitude +
                    " locType=" + locType +
                    " address=" + location.getAddrStr());

            DMApplication.getInstance().setLastActivatedLocation(location);

            // tell subscriber to update location
            EventBus.getDefault().post(new LocationEvent(LocationEvent.LocationEventEnum.UPDATED, location));

            AVUser user = AVUser.getCurrentUser();
            if (user != null) {
                PreferenceMap preferenceMap = new PreferenceMap(DMApplication.getInstance(), user.getObjectId());

                AVGeoPoint avGeoPoint = preferenceMap.getLocation();
                if (avGeoPoint != null && avGeoPoint.getLatitude() == location.getLatitude()
                        && avGeoPoint.getLongitude() == location.getLongitude()) {
                    locClient.stop();
                } else {
                    AVGeoPoint newGeoPoint = new AVGeoPoint(location.getLatitude(),
                            location.getLongitude());
                    preferenceMap.setLocation(newGeoPoint);
                }
            }

            if(isGpsOpen()){
                setGps("定位结束,是否关闭GPS!");
            }

        }
    }
}
