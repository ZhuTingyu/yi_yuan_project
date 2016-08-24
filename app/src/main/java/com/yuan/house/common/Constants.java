package com.yuan.house.common;


import java.util.Locale;

/**
 * Created by Alsor Zhou on 2/13/15.
 */
public class Constants {
    public static final String kServiceCheckUpdate = "checkupdate";

    public static final boolean kDebugCouponFeature = true;

    public static final String kWebServiceHost = "http://www.baidu.com";

    // Development Server
//    public static String kWebServiceEndpoint = "http://house.ieyuan.com";

    public static String kHouseParamsForChatRoom = "kHouseParamsForChatRoom";
    public static String kHouseSwitchParamsForChatRoom = "kHouseSwitchParamsForChatRoom";
    public static Locale kForceLocale = Locale.US;
    // Test Server
    public static String kWebServiceEndpoint = "http://test.house.ieyuan.com";
    public static String kFileServerEndpoint = "http://static.eye5250.com/api";
    public static String kWebServiceAPIEndpoint = kWebServiceEndpoint + "/api";
    /**
     * Web Service Url
     */
    public static String kWebServiceFileUpload = kFileServerEndpoint + "/file-upload/feedback";
    public static String kWebServiceImageUpload = kFileServerEndpoint + "/image-upload";
    public static String kWebServiceSendFeedback = kWebServiceAPIEndpoint + "/feedback";
    public static String kWebServiceGetFeedback = kWebServiceAPIEndpoint + "/feedback?page=";
    public static String kWebServiceGetBrokers = kWebServiceAPIEndpoint + "/agency/complain";
    public static String kWebServiceSwitchable = kWebServiceAPIEndpoint + "/house/switchable/";
    public static String kPrefsFirstLaunch = "kPrefsFirstLaunch";
    public static String kWebPageEntry = "nearby.html";
    public static String kWebPageLogin = "login.html";
    // Web package version
    public static String kWebPackageVersionCached = "kWebPackageVersionCached";
    public static String kWebLaunchImageVersionCached = "kWebLaunchImageVersionCached";
    //    public static String kApplicationLaunchImageVersion = "0.0.1";
    public static String kWebPackageExtracted = "kWebPackageExtracted";
    // Native app version
    public static String kApplicationPackageVersion = "0.0.1";
    public static String kAppHasNewVersion = "hasNewVersionAvailable";
    public static String kAppVersionCode = "versionCode";
    public static String kNewAppDownloadUrl = "kNewAppDownloadUrl";
    /**
     * LeanCloud Product Application Settings.
     */
    public static String kAVApplicationId = "YDKvErwgUXfzN2uN195jluu6-gzGzoHsz";
    public static String kAVClientKey = "T0L2MXf76fHILzPrS1ewUnXN";
    public static String kCheckTypeHtml = "KidsParentHtmlPackage";
    public static String kCheckTypeLaunchImage = "KidsParentLaunchImage";
    /**
     * Fragment Identifiers
     */
    public static String kFragmentTagConversation = "kFragmentTagConversation";
    public static String kFragmentTagContacts = "kFragmentTagContacts";
    public static String kFragmentTagWebView = "kFragmentTagWebView";
    public static String kFragmentTagMain = "kFragmentTagMain";
    public static String kFragmentTagCoupon = "kFragmentTagCoupon";
    public static String kFragmentTagLogin = "kFragmentTagLogin";
    public static String kFragmentTagProposal = "kFragmentTagProposal";
    public static String kFragmentTagMessage = "kFragmentTagMessage";
    public static String kBugTagsKey = "c0a589a1390e94a20f24ce42885311b7";
    public static String kLeanChatCurrentUserObjectId = "kLeanChatCurrentUserObjectId";
    public static String kWebDataKeyUserLogin = "userLogin";
    public static String kHttpReqKeyContentType = "Content-Type";
    public static String kHttpReqKeyAuthToken = "token";
    public static String kFragmentTagBBS = "kFragmentTagBBS";
    public static String kFragmentTagChat = "kFragmentTagChat";

    public static String kWebPageUserCoupon = "user_coupon.html";
    public static String kWebPageAgencyCoupon = "agency_coupon.html";
    public static String kWebPageUserBBS = "user_bbs.html";
    public static String kWebPageAgencyBBS = "agency_bbs.html";
    public static String kWebPageUserIndex = "user_index.html";
    public static String kWebPageAgencyIndex = "agency_index.html";
    public static String kWebPageAgencyMessage = "agency_message.html";
    public static String kWebPageUserMessage = "user_message.html";
    public static String kWebPageUserCenter = "user_center.html";
    public static String kWebPageAgencyCenter = "agency_center.html";
    /**
     * 用于从 Java 代码中调用的 Javascript 方法名
     */
    public static String kJavascriptFnOnRightItemClick = "onRightItemClick";
    public static String kBundleKeyAfterSwitchHouseSelected = "kBundleKeyAfterSwitchHouseSelected";
    public static String kActivityParamFinishSelectLocationOnMap = "kActivityParamFinishSelectLocationOnMap";
    /**
     * Image Crop
     */
    public static String kBundleExtraCropImageType = "cropType";
    public static String kBundleExtraCropImageName = "cropImage";
    public static String kImageCropTypeRectangle = "rectangle";
    public static String kImageCropTypeSquare = "square";
    public static String kImageCropTypeNone = "none";
    public static int kActivityRequestCodeImagePickThenCropRectangle = 12;
    public static int kActivityRequestCodeImagePickThenCropSquare = 13;
    public static String kDateFormatStyleShort = "MM-dd mm:ss";
    public static String kLastActivatedHouseId = "kLastActivatedHouseId";
    public static String kLastActivatedHouseTradeType = "kLastActivatedHouseTradeType";
    public static String kHttpReqKeyGeoCity = "city";
    public static String kHttpReqKeyGeoDistrict = "district";
    public static int kNotifyId = 1;
    public static String kPrefsLastSelectedCityFromMap = "lastCity";
    public static String kPrefsLastSelectedDistrictFromMap = "lastDistrict";
}
