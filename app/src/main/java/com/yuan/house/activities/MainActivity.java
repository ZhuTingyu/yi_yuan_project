package com.yuan.house.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

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
import com.avoscloud.chat.service.UserService;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.utils.StringUtil;
import com.umeng.update.UmengUpdateAgent;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.AuthEvent;
import com.yuan.house.event.LocationEvent;
import com.yuan.house.event.PageEvent;
import com.yuan.house.ui.fragment.AgencyMainFragment;
import com.yuan.house.ui.fragment.AgencyMessageFragment;
import com.yuan.house.ui.fragment.LoginFragment;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.UserMainFragment;
import com.yuan.house.ui.fragment.UserMessageFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.ToastUtil;
import com.yuan.house.R;

import org.json.JSONException;

import java.util.HashMap;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/14.
 */

public class MainActivity extends WebViewBasedActivity implements WebViewFragment.OnFragmentInteractionListener {
    public static MainActivity instance;
    public LocationClient locClient;
    public HouseLocationListener locationListener;

    String cachedNotificationPayload;
    private BottomNavigationBar bottomNavigationBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            cachedNotificationPayload = bundle.getString("payload");
        }

        instance = this;

        // Register event bus to receive events
        EventBus.getDefault().register(this);

        UmengUpdateAgent.update(this);

        setContentView(R.layout.activity_main, false, true);

        Injector.inject(this);

        mContext = this;

        setupTabbarClickListener();

        // Set default Activity when push comes
        PushService.setDefaultPushCallback(this, MainActivity.class);

        // 订阅频道，当该频道消息到来的时候，打开对应的 Activity
        PushService.subscribe(this, "public", MainActivity.class);
        PushService.subscribe(this, "private", MainActivity.class);
        PushService.subscribe(this, "protected", MainActivity.class);

        AVAnalytics.trackAppOpened(getIntent());

        initBaiduLocClient();

        // configure chat service
        if (AVUser.getCurrentUser() != null) {
            ChatManager chatManager = ChatManager.getInstance();
            chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
            chatManager.openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);
            CacheService.registerUser(AVUser.getCurrentUser());
        }

        if (prefs.getString(Constants.kWebDataKeyUserLogin, null) != null) {
            switchToFragment(Constants.kFragmentTagMain);
        } else {
            switchToFragment(Constants.kFragmentTagLogin);
        }
    }

    @Override
    public void onFragmentInteraction(WebViewBaseFragment fragment) {
        super.onFragmentInteraction(fragment);

        if (!TextUtils.isEmpty(cachedNotificationPayload)) {
            callbackWhenGetNotification(cachedNotificationPayload);
        }
    }


    public void onEvent(PageEvent event) {
        if (event.getEventType() == PageEvent.PageEventEnum.FINISHED) {
        }
    }

    public void onEvent(AuthEvent event) {
        if (event.getEventType() == AuthEvent.AuthEventEnum.LOGOUT) {
            // 注销已完成, 重新显示登录界面
//            switchToFragment(Constants.kFragmentTagLogin);
            Intent intent = new Intent(mContext, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Notify JS that APP get a remote notification
     *
     * @param notif notification body
     */
    private void callbackWhenGetNotification(String notif) {
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
        Fragment f = mFragmentManager.findFragmentByTag(tag);

        if (f != null) {
            Timber.i("Found Fragment : " + tag);

            return f;
        }

        if (tag.equals(Constants.kFragmentTagMain)) {
            if (DMApplication.getInstance().iAmUser()) {
                f = UserMainFragment.newInstance();
            } else {
                f = AgencyMainFragment.newInstance();
            }
        } else if (tag.equals(Constants.kFragmentTagMessage)) {
            if (DMApplication.getInstance().iAmUser()) {
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

    public void setupTabbarClickListener() {
        this.bottomNavigationBar = ButterKnife.findById(getTabBar(), R.id.bottom_navigation_bar);
        bottomNavigationBar
                .addItem(new BottomNavigationItem(R.drawable.ic_home, "房源")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_chat, "消息")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_suggest, "建议")).setActiveColor(R.color.primary_color_scheme)
                .setFirstSelectedPosition(0)
                .initialise();

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

    public void switchToFragment(String tag) {
        // use other method to keep the old fragment than use this simple and rude `replace`
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.replace(R.id.content_frame, getFragment(tag), tag);
        mFragmentTransaction.commit();
    }

    public void onBridgeUpdateFriendRelationship() {
        bottomNavigationBar.selectTab(0);
    }

    public void onBridgeDropToMessage() {
        bottomNavigationBar.selectTab(1);
    }

    /**
     * Callback when user has sign in
     *
     * @param data user credentials
     */
    public void onBridgeSignIn(final String data) {
        HashMap<String, String> params;
        try {
            params = StringUtil.JSONString2HashMap(data);

            String key = params.get("key");
            String value = params.get("value");

            SharedPreferences.Editor editor = prefs.edit();
            if (value == null || value.equals("null")) {
                editor.remove(key);
            } else {
                editor.putString(key, value);
            }
            editor.apply();

            if (Constants.kWebDataKeyUserLogin.equals(key)) {
                params = StringUtil.JSONString2HashMap(data);
                params = StringUtil.JSONString2HashMap(params.get("value"));
                if (params.get("user_info") != null) {
                    params = StringUtil.JSONString2HashMap(params.get("user_info"));
                } else {
                    params = StringUtil.JSONString2HashMap(params.get("agency_info"));
                }

                // TODO: 16/6/8 edit user type
                String userName = params.get("lean_user");
                String passwd = params.get("lean_passwd");

                avUserLogin(userName, passwd);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void avUserLogin(final String userName, String userPass) {
        //TODO: handler after login to own server success
        AVUser.logInInBackground(userName, userPass,
                new LogInCallback<AVUser>() {
                    @Override
                    public void done(AVUser avUser, AVException e) {
                        if (avUser != null) {
                            String chatUserId = avUser.getObjectId();
                            prefs.edit().putString("avUserLogin", userName)
                                    .putString(Constants.kLeanChatCurrentUserObjectId, chatUserId)
                                    .apply();
                            UserService.updateUserLocation();
                            ChatManager chatManager = ChatManager.getInstance();
                            chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
                            chatManager.openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);
                            CacheService.registerUser(AVUser.getCurrentUser());

                            switchToFragment(Constants.kFragmentTagMain);
                            getBottomNavigationBar().clearAll();
                            setupTabbarClickListener();
                        } else {
                            ToastUtil.showShort(mContext, "leancould登陆失败");
                        }
                    }
                });
    }

    private void initBaiduLocClient() {
        locClient = new LocationClient(mContext);
        locClient.setDebug(true);
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
                    UserService.updateUserLocation();
                    locClient.stop();
                } else {
                    AVGeoPoint newGeoPoint = new AVGeoPoint(location.getLatitude(),
                            location.getLongitude());
                    preferenceMap.setLocation(newGeoPoint);
                }
            }
        }
    }
}
