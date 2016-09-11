package com.yuan.house.common;


import com.yuan.house.BuildConfig;

import java.util.Locale;

/**
 * Created by Alsor Zhou on 2/13/15.
 */
public class Constants {
    public static final Locale kForceLocale = Locale.US;

    public static final String kHouseParamsForChatRoom = "kHouseParamsForChatRoom";
    public static final String kHouseSwitchParamsForChatRoom = "kHouseSwitchParamsForChatRoom";

    /**
     * Web Service Url
     */
    public static final String kWebServiceAPIEndpoint = BuildConfig.kWebServiceEndpoint + "/api";
    private static final String kFileServerEndpoint = "http://static.eye5250.com/api";
    public static final String kWebServiceFileUpload = kFileServerEndpoint + "/file-upload/feedback";
    public static final String kWebServiceImageUpload = kFileServerEndpoint + "/image-upload";
    public static final String kWebServiceSendFeedback = kWebServiceAPIEndpoint + "/feedback";
    public static final String kWebServiceGetFeedback = kWebServiceAPIEndpoint + "/feedback?page=";
    public static final String kWebServiceGetBrokers = kWebServiceAPIEndpoint + "/agency/complain";
    public static final String kWebServiceSwitchable = kWebServiceAPIEndpoint + "/house/switchable/";
    public static final String kWebServiceHTMLPackageVersion = kWebServiceAPIEndpoint + "/html_package";

    /**
     * LeanCloud Product Application Settings.
     */
    public static final String kAVApplicationId = "YDKvErwgUXfzN2uN195jluu6-gzGzoHsz";
    public static final String kAVClientKey = "T0L2MXf76fHILzPrS1ewUnXN";

    /**
     * Fragment Identifiers
     */
    public static final String kFragmentTagWebView = "kFragmentTagWebView";
    public static final String kFragmentTagMain = "kFragmentTagMain";
    public static final String kFragmentTagCoupon = "kFragmentTagCoupon";
    public static final String kFragmentTagLogin = "kFragmentTagLogin";
    public static final String kFragmentTagProposal = "kFragmentTagProposal";
    public static final String kFragmentTagMessage = "kFragmentTagMessage";
    public static final String kFragmentTagBBS = "kFragmentTagBBS";

    public static final String kBugTagsKey = "c0a589a1390e94a20f24ce42885311b7";
    public static final String kLeanChatCurrentUserObjectId = "kLeanChatCurrentUserObjectId";
    public static final String kWebDataKeyUserLogin = "userLogin";
    public static final String kHttpReqKeyContentType = "Content-Type";
    public static final String kHttpReqKeyAuthToken = "token";

    /**
     * Html Pages
     */
    public static final String kWebPageUserCoupon = "user_coupon.html";
    public static final String kWebPageAgencyCoupon = "agency_coupon.html";
    public static final String kWebPageUserBBS = "user_bbs.html";
    public static final String kWebPageAgencyBBS = "agency_bbs.html";
    public static final String kWebPageUserIndex = "user_index.html";
    public static final String kWebPageUserIndexFirst = "user_index_first.html";
    public static final String kWebPageAgencyIndex = "agency_index.html";
    public static final String kWebPageAgencyMessage = "agency_message.html";
    public static final String kWebPageUserMessage = "user_message.html";
    public static final String kWebPageUserCenter = "user_center.html";
    public static final String kWebPageAgencyCenter = "agency_center.html";
    public static final String kWebPageEntry = "nearby.html";
    public static final String kWebPageLogin = "login.html";

    /**
     * 用于从 Java 代码中调用的 Javascript 方法名
     */
    public static final String kJavascriptFnOnRightItemClick = "onRightItemClick";
    public static final String kBundleKeyAfterSwitchHouseSelected = "kBundleKeyAfterSwitchHouseSelected";
    public static final String kActivityParamFinishSelectLocationOnMap = "kActivityParamFinishSelectLocationOnMap";

    /**
     * Image Crop
     */
    public static final String kBundleExtraCropImageType = "cropType";
    public static final String kBundleExtraCropImageName = "cropImage";
    public static final String kImageCropTypeRectangle = "rectangle";
    public static final String kImageCropTypeSquare = "square";
    public static final String kImageCropTypeNone = "none";
    public static final int kActivityRequestCodeImagePickThenCropRectangle = 12;
    public static final int kActivityRequestCodeImagePickThenCropSquare = 13;
    public static final String kDateFormatStyleShort = "MM-dd mm:ss";
    public static final String kLastActivatedHouseId = "kLastActivatedHouseId";
    public static final String kLastActivatedHouseTradeType = "kLastActivatedHouseTradeType";
    public static final String kHttpReqKeyGeoCity = "city";
    public static final String kHttpReqKeyGeoDistrict = "district";
    public static final int kNotifyId = 1;

    /**
     * Preferences
     */
    public static final String kPrefsHtmlVersionCode = "kPrefsHtmlVersionCode";
    public static final String kPrefsHtmlVersionNo = "kPrefsHtmlVersionNo";
    public static final String kPrefsHtmlMainCode = "kPrefsHtmlMainCode";
    public static final String kPrefsHtmlPackageExtracted = "kPrefsHtmlPackageExtracted";

    public static final String kPrefsFirstLaunch = "kPrefsFirstLaunch";
    public static final String kPrefsAppHasNewVersion = "hasNewVersionAvailable";
    public static final String kPrefsNativeAppVersion = "kPrefsNativeAppVersion";
    public static final String kPrefsNewAppDownloadUrl = "kPrefsNewAppDownloadUrl";

    public static final String kPrefsHasAgencyFriends = "kPrefsHasAgencyFriends";
    public static final String kPrefsLastSelectedCityFromMap = "lastCity";
    public static final String kPrefsLastSelectedDistrictFromMap = "lastDistrict";

    /**
     * HTML Package Version
     */
    public static final String kHtmlMainCodeDefault = "1";
    public static final String kHtmlVersionCodeDefault = "5";
}
