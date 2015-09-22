package com.yuan.skeleton.activities;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVGeoPoint;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.PushService;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.chat.ui.contact.ContactFragment;
import com.avoscloud.chat.ui.conversation.ConversationRecentFragment;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.skeleton.R;
import com.yuan.skeleton.application.DMApplication;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.common.Constants;
import com.yuan.skeleton.event.PageEvent;
import com.yuan.skeleton.ui.fragment.WebViewBaseFragment;
import com.yuan.skeleton.ui.fragment.WebViewFragment;
import com.umeng.update.UmengUpdateAgent;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/14.
 */

public class MainActivity extends WebViewBasedActivity implements WebViewFragment.OnFragmentInteractionListener {

    public LocationClient locClient;
    public TCLocationListener locationListener;

    String cachedNotificationPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            cachedNotificationPayload = bundle.getString("payload");
        }

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
        ChatManager chatManager = ChatManager.getInstance();
        chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
        chatManager.openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);

// FIXME: crash here
//        UpdateService updateService = UpdateService.getInstance(this);
//        updateService.checkUpdate();
        CacheService.registerUser(AVUser.getCurrentUser());

        switchToFragment(Constants.kFragmentTagNearby);
    }

    @Override
    public void onFragmentInteraction(WebViewBaseFragment fragment) {
        super.onFragmentInteraction(fragment);

        // check if we have pending notitfication to handle
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
        bridge.callHandler("callbackWhenGetNotification", notif, new WebViewJavascriptBridge.WVJBResponseCallback() {
            @Override
            public void callback(String data) {
                Timber.v("Transfer notification to JS successed");
            }
        });
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
            f = WebViewFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagSocial)) {
            f = WebViewFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagProfile)) {
            f = WebViewFragment.newInstance();
        } else if (tag.equals(Constants.kFragmentTagConversation)) {
            f = new ConversationRecentFragment();
        } else if (tag.equals(Constants.kFragmentTagContacts)) {
            f = new ContactFragment();
        } else {

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

    // FIXME: ugly implementation, need dynamic adapt the tab bar items.
    private void setupTabbarClickListener() {
        ButterKnife.findById(getTabBar(), R.id.tabbar_btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonTextColor(R.id.tabbar_btn_1);
                changeButtonIconDrawable(R.id.tabbar_btn_1, R.drawable.ic_nearby_p);

                switchToFragment(Constants.kFragmentTagNearby);
            }
        });

        ButterKnife.findById(getTabBar(), R.id.tabbar_btn_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonTextColor(R.id.tabbar_btn_2);
                changeButtonIconDrawable(R.id.tabbar_btn_2, R.drawable.ic_circle_p);

                switchToFragment(Constants.kFragmentTagSocial);
            }
        });

        ButterKnife.findById(getTabBar(), R.id.tabbar_btn_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonTextColor(R.id.tabbar_btn_3);
                changeButtonIconDrawable(R.id.tabbar_btn_3, R.drawable.ic_report_p);

                switchToFragment(Constants.kFragmentTagContacts);
            }
        });

        ButterKnife.findById(getTabBar(), R.id.tabbar_btn_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonTextColor(R.id.tabbar_btn_4);
                changeButtonIconDrawable(R.id.tabbar_btn_4, R.drawable.ic_chat_p);

                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

                ConversationRecentFragment fragment = (ConversationRecentFragment) getFragment(Constants.kFragmentTagConversation);
                mFragmentTransaction.replace(R.id.content_frame, fragment, Constants.kFragmentTagConversation);
                mFragmentTransaction.commit();
            }
        });

        ButterKnife.findById(getTabBar(), R.id.tabbar_btn_5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonTextColor(R.id.tabbar_btn_5);
                changeButtonIconDrawable(R.id.tabbar_btn_5, R.drawable.ic_user_p);

                switchToFragment(Constants.kFragmentTagProfile);
            }
        });
    }

    private void setupDefaultApperance() {
        changeButtonTextColor(R.id.tabbar_btn_1);
    }

    private void changeButtonTextColor(int resId) {
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_1)).setTextColor(getResources().getColor(R.color.text_default_gray));
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_2)).setTextColor(getResources().getColor(R.color.text_default_gray));
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_3)).setTextColor(getResources().getColor(R.color.text_default_gray));
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_4)).setTextColor(getResources().getColor(R.color.text_default_gray));
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_5)).setTextColor(getResources().getColor(R.color.text_default_gray));

        ((Button) ButterKnife.findById(getTabBar(), resId)).setTextColor(getResources().getColor(R.color.primary_color_scheme));
    }

    private void changeButtonIconDrawable(int resId, int resIconId) {
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_1)).setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_nearby), null, null);
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_2)).setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_circle), null, null);
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_3)).setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_report), null, null);
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_4)).setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_chat), null, null);
        ((Button) ButterKnife.findById(getTabBar(), R.id.tabbar_btn_5)).setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_user), null, null);

        ((Button) ButterKnife.findById(getTabBar(), resId)).setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(resIconId), null, null);
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

    private void switchToFragment(String tag) {
        // use other method to keep the old fragment than use this simple and rude `replace`
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.replace(R.id.content_frame, getFragment(tag), tag);
        mFragmentTransaction.commit();
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
