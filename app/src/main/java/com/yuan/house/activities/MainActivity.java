package com.yuan.house.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVGeoPoint;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avos.avoscloud.PushService;
import com.avoscloud.chat.entity.avobject.User;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.utils.NetAsyncTask;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.http.RestClient;
import com.dimo.web.WebViewJavascriptBridge;
import com.umeng.update.UmengUpdateAgent;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.PageEvent;
import com.yuan.house.ui.fragment.AgencyMainFragment;
import com.yuan.house.ui.fragment.AgencyMessageFragment;
import com.yuan.house.ui.fragment.LoginFragment;
import com.yuan.house.ui.fragment.UserMainFragment;
import com.yuan.house.ui.fragment.UserMessageFragment;
import com.yuan.house.ui.fragment.UserProposalFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.skeleton.R;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/14.
 */

public class MainActivity extends WebViewBasedActivity implements WebViewFragment.OnFragmentInteractionListener {
    public static MainActivity instance;
    public LocationClient locClient;
    public TCLocationListener locationListener;

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

        // Register event bus to receive events
        EventBus.getDefault().register(this);

        Timber.v("Installation id: " + AVInstallation.getCurrentInstallation().getInstallationId());

        AVAnalytics.trackAppOpened(getIntent());

        initBaiduLocClient();

        // configure chat service
        if (AVUser.getCurrentUser() != null) {
            ChatManager chatManager = ChatManager.getInstance();
            chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
            chatManager.openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);
            CacheService.registerUser(AVUser.getCurrentUser());
        }

        if (prefs.getBoolean("isLogin", false)) {
            switchToFragment(Constants.kFragmentTagNearby);
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

        if (tag.equals(Constants.kFragmentTagNearby)) {
            if (isUserType())
                f = UserMainFragment.newInstance();
            else
                f = AgencyMainFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagMessage)) {
            if (isUserType()) {
                f = UserMessageFragment.newInstance();
            } else {
                f = AgencyMessageFragment.newInstance();
            }
        } else if (tag.equals(Constants.kFragmentTagLogin)) {
            f = LoginFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagProposal)) {
            f = UserProposalFragment.newInstance();
        } else {
            f = WebViewFragment.newInstance();
        }

        Timber.v("NOT Found Fragment : " + tag + ", need to create!!!");

        return f;
    }

    private void initBaiduLocClient() {
        locClient = new LocationClient(this.getApplicationContext());
        locClient.setDebug(true);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(5000);
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        locClient.setLocOption(option);

        locationListener = new TCLocationListener();
        locClient.registerLocationListener(locationListener);
        locClient.start();
    }

    public BottomNavigationBar getBottomNavigationBar() {
        return bottomNavigationBar;
    }

    // FIXME: ugly implementation, need dynamic adapt the tab bar items.
    public void setupTabbarClickListener() {
        this.bottomNavigationBar = ButterKnife.findById(getTabBar(), R.id.bottom_navigation_bar);
        bottomNavigationBar
                .addItem(new BottomNavigationItem(R.drawable.ic_home, "房源")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_chat, "消息")).setActiveColor(R.color.primary_color_scheme)
                .addItem(new BottomNavigationItem(R.drawable.ic_suggest, "建议")).setActiveColor(R.color.primary_color_scheme)
                .setFirstSelectedPosition(0)
                .initialise();

        if (!prefs.getBoolean("isLogin", false))
            return;

        bottomNavigationBar.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int position) {

                switch (position) {
                    case 0:
                        switchToFragment(Constants.kFragmentTagNearby);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    public void onBridgeSignIn (final String data){
        try {
            final JSONObject userLogin = new JSONObject(data);
            final String chat_service_id = userLogin.has("chat_service_id") ? userLogin.getString("chat_service_id") : null;
            final String chat_service_passwd = userLogin.has("chat_service_passwd") ? userLogin.getString("chat_service_passwd") : null;
            if (TextUtils.isEmpty(chat_service_id)) {
                Timber.e("Empty");
            } else {

            }

            if(userLogin.isNull("chat_user_id") || TextUtils.isEmpty(userLogin.getString("chat_user_id"))) {
                NetAsyncTask task = new NetAsyncTask(MainActivity.this) {
                    @Override
                    protected void doInBack() throws Exception {
                        AVUser user = UserService.signUp(chat_service_id, chat_service_passwd);
                        User.setGender(user, userLogin.getInt("gender")==1? User.Gender.Male:User.Gender.Female);
                        user.setFetchWhenSave(true);
                        user.save();
                    }

                    @Override
                    protected void onPost(Exception e) {
                        if (e != null) {
                            Timber.e(e, "failed on register av user");
                        } else {
                            UserService.updateUserLocation();
                        }
                        final AVUser avUser = AVUser.getCurrentUser();
                        try {
                            JSONObject request = new JSONObject();
                            request.put("url", "/user/"+userLogin.getInt("id"));
                            request.put("headers", new JSONObject("{\"access-key\":\"" + userLogin.getString("access_key") + "\"}"));
                            request.put("data", new JSONObject("{\"chat_user_id\":\"" + avUser.getObjectId() + "\"}"));
                            RestClient.getInstance().bridgeRequest(request, RestClient.METHOD_PUT, new WebViewJavascriptBridge.WVJBResponseCallback() {
                                @Override
                                public void callback(Object data) {
                                    try {
                                        JSONObject response = new JSONObject(data.toString());
                                        if(response.getInt("status")==200) {
                                            userLogin.put("chat_user_id", avUser.getObjectId());
                                            prefs.edit().putString(Constants.kLeanChatCurrentUserObjectId, avUser.getObjectId()).commit();
                                            prefs.edit().putString("avUserLogin", userLogin.toString()).commit();
//                                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
//                                            finish();
                                        }
                                    } catch (JSONException e1) {
                                        e1.printStackTrace();
                                    }
                                    Timber.i("upload chat_user_id response:" + data);
                                }
                            });
                        } catch (Exception e1) {
                            Timber.e(e1, "error on upload chat_user_id");
                        }
                    }
                };

                task.execute();
            } else {
                //TODO: handler after login to own server success
                AVUser.logInInBackground(chat_service_id,
                        chat_service_passwd,
                        new LogInCallback<AVUser>() {
                            @Override
                            public void done(AVUser avUser, AVException e) {
                                if (avUser != null) {
                                    String chatUserId = avUser.getObjectId();
                                    prefs.edit().putString("avUserLogin", data).apply();
                                    prefs.edit().putString(Constants.kLeanChatCurrentUserObjectId, chatUserId).commit();
                                    UserService.updateUserLocation();
                                }

//                                startActivity(new Intent(SignInActivity.this, MainActivity.class));
//                                finish();
                            }
                        });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class TCLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            int locType = location.getLocType();

            Timber.v("onReceiveLocation latitude=" + latitude + " longitude=" + longitude
                    + " locType=" + locType + " address=" + location.getAddrStr());

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
