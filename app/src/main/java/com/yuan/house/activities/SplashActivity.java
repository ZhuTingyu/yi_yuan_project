package com.yuan.house.activities;


import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

import com.avos.avoscloud.AVUser;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationManager;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.UserInfoFactory;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.dimo.utils.FileUtil;
import com.lfy.dao.DaoMaster;
import com.lfy.dao.DaoSession;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.yuan.house.HeartbeatService;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class SplashActivity extends FragmentActivity {
    @Inject
    public SharedPreferences prefs;

    @BindView(R.id.splash_image_view)
    ImageView imageView;
    
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.splash_view);

        Injector.inject(this);
        ButterKnife.bind(this);

        updateSplashImage();

        startService(new Intent(getBaseContext(), HeartbeatService.class));

        new HeavyTaskInBackground().execute();
    }

    private void runHeavyLoadTaskInBackground() {
        configLocalWebPackageSettings();

// FIXME: 16/7/25 uncomment when in production mode
//        if (!prefs.getBoolean(Constants.kWebPackageExtracted, false)) {
            Timber.v("Copy HTML asset to folder");

            FileUtil.copyAssetFolder(getAssets(), "html", DMApplication.getInstance().getHtmlExtractedFolder());

            prefs.edit().putBoolean(Constants.kWebPackageExtracted, true).apply();
//        }

        // TODO: 16/7/21 move this to heavy operation job.
        initDatabase();

        setupChatManager();
    }

    private void configLocalWebPackageSettings() {
        String rootDataFolder = FileUtil.getDataDirectory(getApplicationContext());
        String htmlExtractedFolder = String.format("%s/%s", FileUtil.getDataDirectory(getApplicationContext()), "html");
        String rootPagesFolder = htmlExtractedFolder + "/pages";

        DMApplication.getInstance().setRootDataFolder(rootDataFolder);
        DMApplication.getInstance().setHtmlExtractedFolder(htmlExtractedFolder);
        DMApplication.getInstance().setRootPagesFolder(rootPagesFolder);
    }

    private void initDatabase() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(getApplicationContext(), "chat-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。

        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();

        DMApplication.getInstance().setMessageDao(daoSession.getMessageDao());
    }

    // setup chat related environment
    private void setupChatManager() {
        final ChatManager chatManager = ChatManager.getInstance();
        chatManager.init(this);

        if (AVUser.getCurrentUser() != null) {
            chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
        }

        chatManager.setConversationEventHandler(ConversationManager.getConversationHandler());

        chatManager.setUserInfoFactory(new UserInfoFactory() {
            PreferenceMap preferenceMap = PreferenceMap.getCurUserPrefDao(DMApplication.getInstance());

            @Override
            public UserInfo getUserInfoById(String userId) {
                AVUser user = CacheService.lookupUser(userId);
                UserInfo userInfo = new UserInfo();
                return userInfo;
            }

            @Override
            public void cacheUserInfoByIdsInBackground(List<String> userIds) throws Exception {
                CacheService.cacheUsers(userIds);
            }

            @Override
            public boolean showNotificationWhenNewMessageCome(String selfId) {
                return preferenceMap.isNotifyWhenNews();
            }

            @Override
            public void configureNotification(Notification notification) {
                if (preferenceMap.isVoiceNotify()) {
                    notification.defaults |= Notification.DEFAULT_SOUND;
                }
                if (preferenceMap.isVibrateNotify()) {
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                }
            }
        });
    }

    private void updateSplashImage() {
        Callback callback = new Callback() {
            @Override
            public void onSuccess() {
                Timber.e("SUCCESS to load the splash picture");
            }

            @Override
            public void onError() {
                // what should do if show splash not success
                Timber.e("FAILED to load the splash picture");
            }
        };

        Picasso.with(this).load(R.mipmap.ic_launcher).into(imageView, callback);
    }

    private void enterMainScreen() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);

        finish();
    }

    private class HeavyTaskInBackground extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            runHeavyLoadTaskInBackground();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            enterMainScreen();
        }
    }
}
