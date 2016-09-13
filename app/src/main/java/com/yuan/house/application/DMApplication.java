package com.yuan.house.application;


import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.support.multidex.MultiDexApplication;

import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.avoscloud.chat.entity.avobject.UpdateInfo;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.db.DBHelper;
import com.avoscloud.leanchatlib.model.AVIMPresenceMessage;
import com.avoscloud.leanchatlib.utils.Logger;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.SDKInitializer;
import com.bugtags.library.Bugtags;
import com.dimo.utils.FileUtil;
import com.dimo.utils.StringUtil;
import com.dimo.utils.ZipUtil;
import com.github.kevinsawicki.http.HttpRequest;
import com.karumi.dexter.Dexter;
import com.lfy.dao.DaoMaster;
import com.lfy.dao.DaoSession;
import com.lfy.dao.MessageDao;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;
import com.yuan.house.BuildConfig;
import com.yuan.house.common.Constants;
import com.yuan.house.event.AuthEvent;
import com.yuan.house.http.RestClient;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import timber.log.Timber;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.FROYO;

public class DMApplication extends MultiDexApplication {
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 2;
    public static boolean debug = true;
    private static DMApplication instance;
    @Inject
    SharedPreferences prefs;
    @Inject
    Context mContext;
    String mLatestVersionCode;
    private ThinDownloadManager downloadManager;
    private boolean allowUserToUseFullFeatureVersion = false;
    private String rootDataFolder;
    private String rootPagesFolder;
    private String htmlExtractedFolder;
    private BDLocation lastActivatedLocation;
    private MessageDao messageDao;
    private String mLatestMainCode;
    private String mLatestVersionNo;

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

    public boolean isAllowUserToUseFullFeatureVersion() {
        return allowUserToUseFullFeatureVersion;
    }

    public void setAllowUserToUseFullFeatureVersion(boolean allowUserToUseFullFeatureVersion) {
        this.allowUserToUseFullFeatureVersion = allowUserToUseFullFeatureVersion;
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

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dexter.initialize(this);
        }

        // Perform injection
        Injector.init(getRootModule(), this);

        Utils.fixAsyncTaskBug();

//        if (BuildConfig.DEBUG) {
        Timber.plant(new Timber.DebugTree());
//        } else {
        Bugtags.start(BuildConfig.kBugTagsKey, this, Bugtags.BTGInvocationEventShake);
//        }

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

        editor.putString(Constants.kPrefsNativeAppCode, version);
        editor.putString(Constants.kPrefsNativeAppVersion, Integer.toString(BuildConfig.VERSION_CODE));

        editor.apply();

        // init download manager
        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);

        RestClient.getInstance().setHostname(Constants.kWebServiceAPIEndpoint);

        // https://leancloud.cn/app.html?appid=9hk99pr7gknwj83tdmfbbccqar1x2myge00ulspafnpcbab8#/key
        AVOSCloud.initialize(this, Constants.kAVApplicationId, Constants.kAVClientKey);

        AVObject.registerSubclass(UpdateInfo.class);

        AVOSCloud.setDebugLogEnabled(debug);
        AVAnalytics.enableCrashReport(this, !debug);

        initImageLoader(instance);

        if (debug) {
            openStrictMode();
            Logger.level = Logger.VERBOSE;
        } else {
            Logger.level = Logger.NONE;
        }

        registerMessageTypes();

        SDKInitializer.initialize(getApplicationContext());

        configLocalWebPackageSettings();

        initDatabase();

        determineWebUpdate();
    }

    private void initDatabase() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(getApplicationContext(), "chat-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。

        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();

        DMApplication.getInstance().setMessageDao(daoSession.getMessageDao());
    }

    private void registerMessageTypes() {
        AVIMMessageManager.registerAVIMMessageType(AVIMPresenceMessage.class);
    }

    /**
     * 关闭聊天相关服务
     */
    private void pruneChatManager() {
        AVInstallation installation = AVInstallation.getCurrentInstallation();
        installation.put("user_id", null);
        installation.put("agency_id", null);

        try {
            installation.save();
        } catch (AVException e) {
            e.printStackTrace();
        }

        final ChatManager chatManager = ChatManager.getInstance();
        chatManager.closeWithCallback(new AVIMClientCallback() {
            @Override
            public void done(AVIMClient avimClient, AVIMException e) {
                EventBus.getDefault().post(new AuthEvent(AuthEvent.AuthEventEnum.LOGOUT, null));
            }
        });
    }

    /**
     * 注销当前登录用户
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Constants.kWebDataKeyUserLogin);
        editor.remove(Constants.kWebDataKeyLoginType);
        editor.putBoolean(Constants.kPrefsHasAgencyFriends, false);
        editor.apply();

        pruneChatManager();
    }

    public void kickOut() {
        logout();
    }

    public MessageDao getMessageDao() {
        return messageDao;
    }

    public void setMessageDao(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    public void openStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectAll()
                .penaltyLog()
                //.penaltyDeath()
                .build());
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

    public void setRootDataFolder(String rootDataFolder) {
        this.rootDataFolder = rootDataFolder;
    }

    /**
     * Check if there has new web content available to download
     */
    public void determineWebUpdate() {
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

                            mLatestVersionCode = object.getString("version_code");
                            mLatestMainCode = object.getString("main_version");
                            mLatestVersionNo = object.getString("version_no");

                            String url = object.getString("file_path");

                            downloadTarBallFromUrl(url);
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
    }

    public String getRootPagesFolder() {
        return rootPagesFolder;
    }

    public void setRootPagesFolder(String rootPagesFolder) {
        this.rootPagesFolder = rootPagesFolder;
    }

    public void stopSelf() {
        try {
            DBHelper.getCurrentUserInstance().closeHelper();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configLocalWebPackageSettings() {
        String rootDataFolder = FileUtil.getDataDirectory(getApplicationContext());
        String htmlExtractedFolder = String.format("%s/%s", FileUtil.getDataDirectory(getApplicationContext()), "html");
        String rootPagesFolder = htmlExtractedFolder + "/pages";

        DMApplication.getInstance().setRootDataFolder(rootDataFolder);
        DMApplication.getInstance().setHtmlExtractedFolder(htmlExtractedFolder);
        DMApplication.getInstance().setRootPagesFolder(rootPagesFolder);
    }
}
