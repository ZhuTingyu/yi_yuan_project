package com.yuan.house.application;


import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.multidex.MultiDexApplication;

import com.avos.avoscloud.AVAnalytics;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
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
import com.bugtags.library.Bugtags;
import com.dimo.utils.FileUtil;
import com.github.kevinsawicki.http.HttpRequest;
import com.karumi.dexter.Dexter;
import com.lfy.dao.MessageDao;
import com.yuan.house.BuildConfig;
import com.yuan.house.common.Constants;
import com.yuan.house.event.AuthEvent;
import com.yuan.house.http.RestClient;
import com.yuan.house.service.InitializeService;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.FROYO;

public class DMApplication extends MultiDexApplication {
    private static DMApplication instance;
    @Inject
    SharedPreferences prefs;
    @Inject
    Context mContext;

    private boolean allowUserToUseFullFeatureVersion = false;
    private String rootDataFolder;
    private String rootPagesFolder;
    private String htmlExtractedFolder;
    private BDLocation lastActivatedLocation;
    private MessageDao messageDao;

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

        RestClient.getInstance().setHostname(Constants.kWebServiceAPIEndpoint);

        // https://leancloud.cn/app.html?appid=9hk99pr7gknwj83tdmfbbccqar1x2myge00ulspafnpcbab8#/key
        AVOSCloud.initialize(this, Constants.kAVApplicationId, Constants.kAVClientKey);

        AVObject.registerSubclass(UpdateInfo.class);

        AVOSCloud.setDebugLogEnabled(BuildConfig.useDebug);
        AVAnalytics.enableCrashReport(this, !BuildConfig.useDebug);

        InitializeService.start(this);

        if (BuildConfig.useDebug) {
            openStrictMode();
            Logger.level = Logger.VERBOSE;
        } else {
            Logger.level = Logger.NONE;
        }

        registerMessageTypes();

        configLocalWebPackageSettings();
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
