package com.yuan.house.application;


import android.app.Application;
import android.app.Instrumentation;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v4.BuildConfig;

import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.avoscloud.chat.entity.avobject.AddRequest;
import com.avoscloud.chat.entity.avobject.UpdateInfo;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationManager;
import com.avoscloud.chat.service.PreferenceMap;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.UserInfoFactory;
import com.avoscloud.leanchatlib.db.DBHelper;
import com.avoscloud.leanchatlib.model.UserInfo;
import com.avoscloud.leanchatlib.utils.Logger;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.SDKInitializer;
import com.dimo.utils.FileUtil;
import com.dimo.utils.StringUtil;
import com.dimo.utils.ZipUtil;
import com.github.kevinsawicki.http.HttpRequest;
import com.lfy.dao.DaoMaster;
import com.lfy.dao.DaoSession;
import com.lfy.dao.MessageDao;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;
import com.yuan.house.activities.SplashActivity;
import com.yuan.house.common.Constants;
import com.yuan.house.event.AuthEvent;
import com.yuan.house.http.RestClient;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.FROYO;

public class DMApplication extends Application {
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 2;
    public static boolean debug = true;
    private static DMApplication instance;
    @Inject
    SharedPreferences prefs;
    @Inject
    Context mContext;
    String mLatestVersion;
    private ThinDownloadManager downloadManager;
    private DaoSession daoSession;
    private String htmlExtractedFolder;
    private String rootDataFolder;
    private String rootPagesFolder;

    private BDLocation lastActivatedLocation;

    /**
     * Create main application
     */
    public DMApplication() {
        // Disable http.keepAlive on Froyo and below
        if (SDK_INT <= FROYO)
            HttpRequest.keepAlive(false);
    }

    /**
     * Create main application
     *
     * @param context
     */
    public DMApplication(final Context context) {
        this();
        attachBaseContext(context);
    }

    /**
     * Create main application
     *
     * @param instrumentation
     */
    public DMApplication(final Instrumentation instrumentation) {
        this();
        attachBaseContext(instrumentation.getTargetContext());
    }

    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                context)
                .threadPoolSize(3).threadPriority(Thread.NORM_PRIORITY - 2)
                //.memoryCache(new WeakMemoryCache())
                .denyCacheImageMultipleSizesInMemory()
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .build();
        ImageLoader.getInstance().init(config);
    }

    public static DMApplication getInstance() {
        return instance;
    }

    public BDLocation getLastActivatedLocation() {
        return lastActivatedLocation;
    }

    public void setLastActivatedLocation(BDLocation lastActivatedLocation) {
        this.lastActivatedLocation = lastActivatedLocation;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // Perform injection
        Injector.init(getRootModule(), this);
        initDatabase();

        Utils.fixAsyncTaskBug();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = pInfo.versionName;

        SharedPreferences.Editor editor = prefs.edit();
        if (prefs.getBoolean(Constants.kPrefsFirstLaunch, true)) {
            Timber.i("Register preference defaults when FIRST LAUNCH!!");

            editor.putBoolean(Constants.kPrefsFirstLaunch, false);

        } else {
            Timber.v("Not First launch");
        }

        editor.putString(Constants.kApplicationPackageVersion, version);

        editor.commit();

//        Bugtags.start(Constants.kBugTagsKey, this, Bugtags.BTGInvocationEventBubble);

//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(Constants.kApplicationId, PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                String sign = Base64.encodeToString(md.digest(), Base64.DEFAULT);
//                Timber.e("KEY HASH:" + sign);
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }

        // init download manager
        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);

        // check web package at every launch
//        determineWebUpdate(Constants.kWebPackageVersionCached, Constants.kCheckTypeHtml);
//        determineWebUpdate(Constants.kWebLaunchImageVersionCached, Constants.kCheckTypeLaunchImage);

        RestClient.getInstance().setHostname(Constants.kWebServiceAPIEndpoint);

        // https://leancloud.cn/app.html?appid=9hk99pr7gknwj83tdmfbbccqar1x2myge00ulspafnpcbab8#/key
        AVOSCloud.initialize(this, Constants.kAVApplicationId, Constants.kAVClientKey);

        // 启用崩溃错误统计
        AVAnalytics.enableCrashReport(mContext, true);
        AVOSCloud.setDebugLogEnabled(true);

        AVObject.registerSubclass(AddRequest.class);
        AVObject.registerSubclass(UpdateInfo.class);

        AVInstallation.getCurrentInstallation().saveInBackground();
        Timber.v("Installation id: " + AVInstallation.getCurrentInstallation().getInstallationId());

        String avInstallId = AVInstallation.getCurrentInstallation().getInstallationId();
        prefs.edit().putString("AVInstallationId", avInstallId).apply();

        PushService.setDefaultPushCallback(instance, SplashActivity.class);
        AVOSCloud.setDebugLogEnabled(debug);
        AVAnalytics.enableCrashReport(this, !debug);
//        AVIMClient.setOfflineMessagePush(true);

        initImageLoader(instance);

        initBaiduMapSDK();

        if (debug) {
            openStrictMode();
        }

        setupChatManager();

        if (debug) {
            Logger.level = Logger.VERBOSE;
        } else {
            Logger.level = Logger.NONE;
        }

        // 使用 assets 目录中的文件
        rootDataFolder = FileUtil.getDataDirectory(getApplicationContext());
        htmlExtractedFolder = String.format("%s/%s", FileUtil.getDataDirectory(getApplicationContext()), "html");

        rootPagesFolder = htmlExtractedFolder + "/pages";

//        if (!prefs.getBoolean(Constants.kWebPackageExtracted, false)) {
        Timber.v("Copy HTML asset to folder : " + htmlExtractedFolder);

        FileUtil.copyAssetFolder(getAssets(), "html", htmlExtractedFolder);
        prefs.edit().putBoolean(Constants.kWebPackageExtracted, true).commit();
//        }
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
            PreferenceMap preferenceMap = PreferenceMap.getCurUserPrefDao(DMApplication.this);

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

    /**
     * 关闭聊天相关服务
     */
    private void pruneChatManager() {
        AVInstallation installation = AVInstallation.getCurrentInstallation();
        installation.put("user_id", null);
        installation.put("agency_id", null);
        installation.saveInBackground();

        final ChatManager chatManager = ChatManager.getInstance();
        chatManager.closeWithCallback(new AVIMClientCallback() {
            @Override
            public void done(AVIMClient avimClient, AVIMException e) {
                Timber.v("LeanMessage : user logout success.");

                EventBus.getDefault().post(new AuthEvent(AuthEvent.AuthEventEnum.LOGOUT, null));
            }
        });
    }

    /**
     * 注销当前登录用户
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.kWebDataKeyLoginType, null);
        editor.putString(Constants.kWebDataKeyUserLogin, null);
        editor.apply();

        pruneChatManager();
    }

    public void kickOut() {
        logout();
    }

    private void initDatabase() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(getApplicationContext(), "chat-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。

        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }

    public MessageDao getMessageDao() {
        return daoSession.getMessageDao();
    }

    public void openStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                //.penaltyDeath()
                .build());
    }

    private void initBaiduMapSDK() {
        SDKInitializer.initialize(getApplicationContext());
    }

    private Object getRootModule() {
        return new RootModule();
    }

    public String getHtmlExtractedFolder() {
        return htmlExtractedFolder;
    }

    public void setHtmlExtractedFolder(String htmlExtractedFolder) {
        this.htmlExtractedFolder = htmlExtractedFolder;
    }

    public String getRootDataFolder() {
        return rootDataFolder;
    }

    /**
     * Check if there has new web content available to download
     */
    public void determineWebUpdate(String prefsKey, final String product) {
        String cachedVersion = prefs.getString(prefsKey, "0.0.1");

        RequestParams requestParams = new RequestParams();
        requestParams.put("product", product);
        requestParams.put("version", cachedVersion);

        RestClient.getInstance().get(Constants.kServiceCheckUpdate, null, requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                // download latest web package
                if (response != null) {
                    String url;
                    try {
                        JSONObject result = response.getJSONObject("Result");

                        mLatestVersion = result.getString("Version");

                        url = result.getString("DownloadUrl");

                        if (product.equals(Constants.kCheckTypeLaunchImage)) {
                            RestClient.getInstance().get(url, null, new FileAsyncHttpResponseHandler(mContext) {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, File response) {
                                    Timber.v("Download Launch Image Success");

                                    // overwrite the launch image locally
                                    File target = new File(FileUtil.getLaunchImagePath(mContext));

                                    try {
                                        FileUtil.copyFile(response, target);
                                    } catch (IOException e) {
                                        Timber.e("ERROR: copy the launch image to data folder");

                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                                    Timber.e("Download Launch Image Failed");
                                }
                            });
                        } else {
                            downloadTarBallFromUrl(url);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    final Uri destinationUri = Uri.parse(getExternalCacheDir().toString() + "/demo.zip");

                    extractZipFileToDocumentFolder(destinationUri.getPath());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
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
                        Timber.v("onDownloadComplete");

                        extractZipFileToDocumentFolder(destinationUri.getPath());
                    }

                    @Override
                    public void onDownloadFailed(int id, int errorCode, String errorMessage) {
                        Timber.e("onDownloadFailed");
                    }

                    @Override
                    public void onProgress(int i, long l, int i1) {
//                        Timber.v("onProgress " + l);
                    }
                });

        downloadManager.add(downloadRequest);
    }

    private void extractZipFileToDocumentFolder(String destUri) {
        String targetFolder = FileUtil.getDataDirectory(this);

        Timber.v("From : " + destUri + " To : " + targetFolder);

        ZipUtil.unzip(destUri, targetFolder);

        prefs.edit().putString(Constants.kApplicationPackageVersion, mLatestVersion).commit();
    }

    public String getRootPagesFolder() {
        return rootPagesFolder;
    }

    public void setRootPagesFolder(String rootPagesFolder) {
        this.rootPagesFolder = rootPagesFolder;
    }

    public boolean iAmUser() {
        return "user".equals(prefs.getString(Constants.kWebDataKeyLoginType, ""));
    }

    public void stopSelf() {
        DBHelper.getCurrentUserInstance().closeHelper();
    }
}
