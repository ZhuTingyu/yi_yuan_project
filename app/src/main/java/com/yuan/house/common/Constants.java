package com.yuan.house.common;


/**
 * Created by Alsor Zhou on 2/13/15.
 */
public class Constants {
    public static String kApplicationId = "com.mowelltech.threec";

    public static String kWebServiceAPIEndpoint = "http://house.ieyuan.com/api";

    public static String kWebServiceFileUpload = kWebServiceAPIEndpoint + "/file-upload/feedback";
    public static String kWebServiceUploadCommon = kWebServiceAPIEndpoint + "/file-upload/common";
    public static String kWebServiceSendFeedback = kWebServiceAPIEndpoint + "/feedback";
    public static String kWebServiceSwitchable = kWebServiceAPIEndpoint + "/house/switchable/";

    // Production Server
    public static String kPrefsFirstLaunch = "kPrefsFirstLaunch";

    public static int kSplashTimeInterval = 20;

    public static String kLocalWebHTMLFolder = "file:///android_asset/html";

    public static String kLocalWebHTMLPageFolder = "file:///android_asset/3c_html";

//TODO: not working with query parameters..
    public static String kWebPageEntry = "nearby.html";
    public static String kWebPageLogin = "login.html";

    // Web package version
    public static String kWebPackageVersionCached = "kWebPackageVersionCached";
    public static String kWebLaunchImageVersionCached = "kWebLaunchImageVersionCached";

    public static String kWebPackageExtracted = "kWebPackageExtracted";

    // Native app version
    public static String kApplicationPackageVersion = "0.0.1";
//    public static String kApplicationLaunchImageVersion = "0.0.1";

    /**
     * LeanCloud Application Settings.
     * https://leancloud.cn/app.html?appid=9hk99pr7gknwj83tdmfbbccqar1x2myge00ulspafnpcbab8#/key
     */
    public static String kAVApplicationId = "IwzlUusBdjf4bEGlypaqNRIx-gzGzoHsz";
    public static String kAVClientKey = "KLhHUoBqw5G1uMjrTsEqbaVR";

    public static final String kServiceCheckUpdate = "checkupdate";
    public static final String kServiceLogin = "login";

    public static String kCheckTypeHtml = "KidsParentHtmlPackage";
    public static String kCheckTypeLaunchImage = "KidsParentLaunchImage";

    /**
     * Fragment Identifiers
     */
    public static String kFragmentTagConversation = "kFragmentTagConversation";
    public static String kFragmentTagContacts = "kFragmentTagContacts";
    public static String kFragmentTagWebView = "kFragmentTagWebView";
    public static String kFragmentTagPopUpSelection = "kFragmentTagPopUpSelection";

    public static String kServiceHost = "http://3c.ieyuan.com";
    public static String kServiceUploadImage = "/upload-image";
    public static String kFragmentTagNearby = "kFragmentTagNearby";
    public static String kFragmentTagLogin = "kFragmentTagLogin";
    public static String kFragmentTagProposal = "kFragmentTagProposal";
    public static String kFragmentTagMessage = "kFragmentTagMessage";
    public static String kFragmentTagSocial = "kFragmentTagSocial";
    public static String kFragmentTagProfile = "kFragmentTagProfile";
    public static String kBugTagsKey = "8b503a091aab6e57b03e1df7d01c1b85";

    public static String kLeanChatCurrentUserObjectId = "kLeanChatCurrentUserObjectId";

    public static String kWebDataKeyUserLogin = "userLogin";
    public static String kWebDataKeyLoginType = "LoginType";
    public static String kWebDataKeyTags = "Tags";

}
