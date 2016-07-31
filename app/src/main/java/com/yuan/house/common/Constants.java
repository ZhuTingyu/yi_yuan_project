package com.yuan.house.common;


/**
 * Created by Alsor Zhou on 2/13/15.
 */
public class Constants {
    public static final String kServiceCheckUpdate = "checkupdate";
    public static final String kServiceLogin = "login";

    // Development Server
//    public static String kWebServiceAPIEndpoint = "http://house.ieyuan.com/api";

    // Test Server
    public static String kWebServiceEndpoint = "http://test.house.ieyuan.com";
    public static String kFileServerEndpoint = "http://static.eye5250.com/api";
    public static String kWebServiceAPIEndpoint = kWebServiceEndpoint + "/api";
    public static String kWebServiceUpdata = "http://192.168.1.20:8095/api/";

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
     * LeanCloud Application Settings.
     * https://leancloud.cn/app.html?appid=9hk99pr7gknwj83tdmfbbccqar1x2myge00ulspafnpcbab8#/key
     */
    public static String kAVApplicationId = "IwzlUusBdjf4bEGlypaqNRIx-gzGzoHsz";
    public static String kAVClientKey = "KLhHUoBqw5G1uMjrTsEqbaVR";

    public static String kCheckTypeHtml = "KidsParentHtmlPackage";
    public static String kCheckTypeLaunchImage = "KidsParentLaunchImage";

    /**
     * Fragment Identifiers
     */
    public static String kFragmentTagConversation = "kFragmentTagConversation";
    public static String kFragmentTagContacts = "kFragmentTagContacts";
    public static String kFragmentTagWebView = "kFragmentTagWebView";

    public static String kFragmentTagMain = "kFragmentTagMain";
    public static String kFragmentTagLogin = "kFragmentTagLogin";
    public static String kFragmentTagProposal = "kFragmentTagProposal";
    public static String kFragmentTagMessage = "kFragmentTagMessage";

    public static String kBugTagsKey = "c0a589a1390e94a20f24ce42885311b7";

    public static String kLeanChatCurrentUserObjectId = "kLeanChatCurrentUserObjectId";

    public static String kWebDataKeyUserLogin = "userLogin";
    public static String kWebDataKeyLoginType = "LoginType";

    public static String  kHttpReqKeyContentType = "Content-Type";
    public static String  kHttpReqKeyAuthToken = "token";

    public static String kFragmentTagBBS = "kFragmentTagBBS";
    public static String kFragmentTagChat = "kFragmentTagChat";
    public static String kWebPageUserBBS = "user_bbs.html";
    public static String kWebPageAgencyBBS = "agency_bbs.html";
    public static String kWebPageUserIndex = "user_index.html";
    public static String kWebPageAgencyIndex = "agency_index.html";


    /**
     * 用于从 Java 代码中调用的 Javascript 方法名
     */
    public static String kJavascriptFnOnRightItemClick = "onRightItemClick";
    public static String kWebPageAgencyMessage = "agency_message.html";
    public static String kWebpageUserMessage = "user_message.html";
    public static String kWebpageUserCenter = "user_center.html";
    public static String kWebpageAgencyCenter = "agency_center.html";

    public static final String kHouseParamsForChatRoom = "kHouseParamsForChatRoom";
    public static final String kHouseSwitchParamsForChatRoom = "kHouseSwitchParamsForChatRoom";

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
    public static String kPrefsLastSelectedCity = "lastCity";
    public static String kPrefsLastSelectedDistrict = "lastDistrict";
}
