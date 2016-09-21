package com.yuan.house.activities;


import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.ImageView;

import com.avos.avoscloud.AVUser;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationManager;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.UserInfoFactory;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.dimo.utils.FileUtil;
import com.dimo.utils.StringUtil;
import com.dimo.utils.ZipUtil;
import com.lfy.dao.DaoMaster;
import com.lfy.dao.DaoSession;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;
import com.yuan.house.BuildConfig;
import com.yuan.house.HeartbeatService;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.http.RestClient;
import com.yuan.house.ui.dialog.ProgressDialogFragment;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import timber.log.Timber;

public class SplashActivity extends FragmentActivity {
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 2;

    @Inject
    public SharedPreferences prefs;

    @BindView(R.id.splash_image_view)
    ImageView imageView;

    String mLatestVersionCode;
    private String mLatestMainCode;
    private String mLatestVersionNo;

    private ThinDownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setContentView(R.layout.splash_view);

        Injector.inject(this);
        ButterKnife.bind(this);

        updateSplashImage();

        startService(new Intent(getBaseContext(), HeartbeatService.class));

        new HeavyTaskInBackground().execute();

        // init download manager
        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);
    }

    /**
     * Copy web content (html/*) to documents
     */
    private void copyWebContentToDocuents() {
// FIXME: 16/7/25 uncomment when in production mode
//        if (!prefs.getBoolean(Constants.kPrefsHtmlPackageExtracted, false)) {
        Timber.v("Copy HTML asset to folder");

        FileUtil.copyAssetFolder(getAssets(), "html", DMApplication.getInstance().getHtmlExtractedFolder());

        prefs.edit().putBoolean(Constants.kPrefsHtmlPackageExtracted, true).apply();
//        }
    }

    /**
     * Setup chat environment
     */
    private void setupChatManager() {
        final ChatManager chatManager = ChatManager.getInstance();
        chatManager.init();

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

    /**
     * Only set the native version update flag
     */
    private void onlineCheckIfHasNewNativeVersionToDownload() {
        final SharedPreferences.Editor editor = prefs.edit();

        String url = Constants.kWebServiceAPIEndpoint + "/app/v";
        RestClient.getInstance().get(url, null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject obj = new JSONObject(new String(responseBody));

                    Iterator<String> keys = obj.keys();
                    ArrayList<String> data = new ArrayList<>();
                    while (keys.hasNext()) {
                        data.add(obj.getString(keys.next()));
                    }

                    if (data.size() == 0) return;

                    //服务端版本
                    String hasNewVersion;

                    String version = data.get(0);
                    String url = data.get(1);

                    if (TextUtils.isEmpty(version)) {
                        hasNewVersion = "0";
                    } else {
                        int onlineVersionCode = Integer.parseInt(version);

                        int packageVersion = Integer.parseInt(prefs.getString(Constants.kPrefsNativeAppVersion, "0"));
                        if (packageVersion < onlineVersionCode) {
                            hasNewVersion = "1";
                        } else {
                            hasNewVersion = "0";
                        }
                    }

                    editor.putString(Constants.kPrefsAppHasNewVersion, hasNewVersion);
                    editor.putString(Constants.kPrefsNewAppDownloadUrl, url);
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Timber.e("APP Check Version Error");
            }
        });
    }

    /**
     * Check if there has new web content available to download
     */
    public void onlineCheckIfHasNewWebContentVersionToDownload() {
        String cachedMain = prefs.getString(Constants.kPrefsHtmlMainCode, null);
        String cachedVersion = prefs.getString(Constants.kPrefsHtmlVersionCode, null);

        if (StringUtils.isEmpty(cachedMain) || StringUtils.isEmpty(cachedVersion)) {
            cachedMain = BuildConfig.kHtmlMainCodeDefault;
            cachedVersion = BuildConfig.kHtmlVersionCodeDefault;
        }

        String requestUrl = Constants.kWebServiceHTMLPackageVersion + "/" + cachedMain + "/" + cachedVersion;

        OkHttpUtils.get().url(requestUrl)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(response);

                        boolean available = object.getBooleanValue("upgrade");

                        if (available) {
                            ProgressDialogFragment.show(getSupportFragmentManager(), R.string.dlg_detect_web_content_update);

                            mLatestVersionCode = object.getString("version_code");
                            mLatestMainCode = object.getString("main_version");
                            mLatestVersionNo = object.getString("version_no");

                            String url = object.getString("file_path");

                            downloadTarBallFromUrl(url);
                        } else {
                            copyWebContentToDocuents();

                            // enter main screen when copy html/* completed
                            enterMainScreen();
                        }
                    }
                });
    }

    /**
     * Download the web package / launch picture zip from site and save to external cache dir
     * <p/>
     * https://android-arsenal.com/details/1/1077
     */
    public void downloadTarBallFromUrl(String url) {
        if (url.isEmpty()) {
            url = "http://ipv4.download.thinkbroadband.com/5MB.zip";
        }

        Uri downloadUri = Uri.parse(url);
        final Uri destinationUri = Uri.parse(FileUtil.getDataDirectory(this) + "/" + StringUtil.UUID() + ".zip");
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri)
                .setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void onDownloadComplete(int id) {
                        extractZipFileToDocumentFolder(destinationUri.getPath());
                    }

                    @Override
                    public void onDownloadFailed(int id, int errorCode, String errorMessage) {
                        Timber.e("onDownloadFailed");
                    }

                    @Override
                    public void onProgress(int i, long l, int i1) {
                    }
                });

        downloadManager.add(downloadRequest);
    }

    private void extractZipFileToDocumentFolder(String destUri) {
        String targetFolder = FileUtil.getDataDirectory(this);

        ZipUtil.unzip(destUri, targetFolder);

        prefs.edit()
                .putString(Constants.kPrefsHtmlMainCode, mLatestMainCode)
                .putString(Constants.kPrefsHtmlVersionCode, mLatestVersionCode)
                .putString(Constants.kPrefsHtmlVersionNo, mLatestVersionNo)
                .apply();

        ProgressDialogFragment.hide(getSupportFragmentManager());
        enterMainScreen();
    }

    private void initDatabase() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(getApplicationContext(), "chat-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。

        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();

        DMApplication.getInstance().setMessageDao(daoSession.getMessageDao());
    }

    private class HeavyTaskInBackground extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            initDatabase();

            onlineCheckIfHasNewNativeVersionToDownload();

            onlineCheckIfHasNewWebContentVersionToDownload();

            setupChatManager();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
