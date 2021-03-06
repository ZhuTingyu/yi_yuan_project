package com.yuan.house.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.baidu.location.BDLocation;
import com.blankj.utilcode.utils.KeyboardUtils;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.lfy.dao.MessageDao;
import com.yuan.house.BuildConfig;
import com.yuan.house.R;
import com.yuan.house.activities.WebViewBasedActivity;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.BridgeCallbackEvent;
import com.yuan.house.event.WebBroadcastEvent;
import com.yuan.house.http.RestClient;
import com.yuan.house.ui.view.PickerPopWindow;
import com.yuan.house.utils.ToastUtil;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * Created by Alsor Zhou on 8/13/15.
 */
public class WebViewBaseFragment extends Fragment implements WebViewJavascriptBridge.OnBridgeWebViewListener {
    protected WebViewJavascriptBridge bridge;
    protected OnFragmentInteractionListener mFragmentListener;
    protected OnBridgeInteractionListener mBridgeListener;
    protected String mUrl;
    @Inject
    protected SharedPreferences prefs;
    WebViewJavascriptBridge.WVJBResponseCallback mCallback;
    @BindView(R.id.webview)
    WebView mWebView;
    HashMap<String, String> additionalHttpHeaders;
    Unbinder unbinder;
    private PickerPopWindow pickPopWin;

    public HashMap<String, String> getAdditionalHttpHeaders() {
        return additionalHttpHeaders;
    }

    public WebView getWebView() {
        return mWebView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        mUrl = arguments.getString("url");

        dispatchHardCodeUrl();
    }

    private void dispatchHardCodeUrl() {
        if (mUrl == null || getBridge() == null) return;

        if (mUrl.contains("agency_check_contractTwo")) {
            getBridge().callHandler("AuditorNotification", null);
        } else if (mUrl.contains("agency_check_house")) {
            getBridge().callHandler("AuditorNotification", null);
        } else if (mUrl.contains("agency_check_contract")) {
            getBridge().callHandler("AuditorNotification", null);
        }
    }

    @Override
    public void onDestroyView() {
        WebStorage.getInstance().deleteAllData();

        EventBus.getDefault().unregister(this);

        super.onDestroyView();

        ViewGroup holder = ButterKnife.findById(getActivity(), R.id.webview_parent);
        if (holder != null) {
            holder.removeView(mWebView);

            if (mWebView != null) {
                mWebView.removeAllViews();
                mWebView.destroy();
            }
        }

        unbinder.unbind();
    }

    public View createView(LayoutInflater inflater, int resId, ViewGroup container, Bundle savedInstanceState) {
//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
//                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        View view = inflater.inflate(resId, container, false);

        Injector.inject(this);

        unbinder = ButterKnife.bind(this, view);

        Timber.v("onCreateView");

        if (mFragmentListener != null) {
            bridge = new WebViewJavascriptBridge(this, mWebView, null);

            mFragmentListener.onFragmentInteraction(this);

            mWebView.setInitialScale(getResources().getDisplayMetrics().widthPixels * 100 / 360);

            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setDomStorageEnabled(true);

            mWebView.getSettings().setAllowFileAccess(true);
            mWebView.setWebChromeClient(new WebChromeClient());
            mWebView.setHorizontalScrollBarEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }

            if (!TextUtils.isEmpty(mUrl)) {
                redirectToLoadUrl(mUrl, additionalHttpHeaders);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EventBus.getDefault().register(this);

        // register all the web handlers at once
        registerHandle();

        getBridge().registerHandler("getParams", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getParams got:" + data);

                HashMap<String, String> map = getAdditionalHttpHeaders();

                JSONObject object = null;

                if (null != map) {
                    String mapString = map.get("params");

                    try {
                        object = new JSONObject(mapString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (null != callback) {
                    String ret = "";
                    if (object != null) {
                        ret = object.toString();
                    }

                    callback.callback(ret);
                }
            }
        });
    }

    public WebViewJavascriptBridge getBridge() {
        return bridge;
    }

    protected void registerHandle() {
        getBridge().registerHandler("purchase", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Log.i("reponse.data", data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeRequestPurchase(data, callback);
                }
            }
        });

        getBridge().registerHandler("redirectPage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("redirectPage got:" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = object.optString("url");
                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeOpenNewLink(url, object);
                }

                if (null != callback) {
                    callback.callback("redirectPage answer");
                }
            }
        });

        // This will NOT working since the webview might be not ready!!!
        // getBridge().callHandler("testJavascriptHandler", "42");
        getBridge().registerHandler("replacePage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("replacePage got:" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = object.optString("url");
                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeReplaceLink(url, object);
                }

                if (null != callback) {
                    callback.callback("replacePage answer");
                }
            }
        });

        getBridge().registerHandler("showToast", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                if (getActivity() != null) {
                    ((WebViewBasedActivity) getActivity()).onBridgeDismissProgressDialog();

                    com.alibaba.fastjson.JSONObject object = JSON.parseObject(data);
                    ToastUtil.showShort(getActivity(), object.getString("msg"));
                }
            }
        });

        getBridge().registerHandler("shareOnSocial", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                try {
                    JSONObject object = new JSONObject(data);
                    String title = object.optString("title");
                    String content = object.optString("content");
                    String url = object.optString("url");

                    ((WebViewBasedActivity) getActivity()).shareOnSocial(url, title, content);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        getBridge().registerHandler("getClipboardText", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                if (null != callback) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);

                    callback.callback(clipboard.getText());
                }
            }
        });

        getBridge().registerHandler("getAPPVersion", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                if (null != callback) {
                    String version = BuildConfig.VERSION_NAME;
                    callback.callback(version);
                }
            }
        });

        getBridge().registerHandler("showConfirmDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showConfirmDialog got:" + data);

                JSONObject params;
                String msg = null;
                try {
                    params = new JSONObject(data);
                    msg = params.optString("msg");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new AlertDialog.Builder(getActivity())
                        .setMessage(msg)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show();
            }
        });

        getBridge().registerHandler("showProgressDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showProgressDialog got:" + data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeShowProgressDialog();
                }
            }
        });

        getBridge().registerHandler("dismissProgressDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("dismissProgressDialog got:" + data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeDismissProgressDialog();
                }
            }
        });

        getBridge().registerHandler("showErrorMsg", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showErrorMsg" + data);

                try {
                    JSONObject object = new JSONObject(data);
                    if (null != mBridgeListener) {
                        mBridgeListener.onBridgeShowErrorMessage(object);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        getBridge().registerHandler("updatePackage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("updatePackage got:" + data);
                if (null != callback) {
                    callback.callback("updatePackage answer");
                }
            }
        });

        getBridge().registerHandler("getCurrentPackageVersion", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getCurrentPackageVersion got:" + data);
                if (null != callback) {
                    String version = prefs.getString(Constants.kPrefsNativeAppCode, "100");

                    callback.callback(version);
                }
            }
        });
        getBridge().registerHandler("setTitle", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setTitle got:" + data);
                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String title = object.optString("text");

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeSetTitle(title);
                }
            }
        });
        getBridge().registerHandler("hideRightItem", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("hideRightItem");

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeHideRightItem();
                }
            }
        });

        getBridge().registerHandler("setRightItem", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setRightItem got:" + data);
                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final String text = object.optString("type");
                final String content = object.optString("content");
                if (!TextUtils.isEmpty(text) && text.equals("icon")) {
                    if (isAdded()) {
                        Resources resources = getResources();
                        String icon = content.substring(0, object.optString("content").indexOf("."));
                        int resourceId = resources.getIdentifier(icon, "drawable", getActivity().getPackageName());
                        if (null != mBridgeListener) {
                            mBridgeListener.onBridgeSetRightItem(resourceId, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    getBridge().callHandler("onRightItemClick");
                                }
                            });
                        }
                    }
                } else {
                    // show a text button
                    if (null != mBridgeListener) {
                        mBridgeListener.onBridgeSetRightItem(content, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getBridge().callHandler("onRightItemClick", content, null);
                            }
                        });
                    }
                }
            }
        });

        getBridge().registerHandler("callNumber", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("callNumber got:" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (null != object) {
                    final String phone = object.optString("phone");
                    // TODO: check if phone number is valid.
                    if (!TextUtils.isEmpty(phone)) {
                        String uri = "tel:" + phone;
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse(uri));
                        startActivity(intent);
                    }
                    if (null != callback) {
                        callback.callback("Call number callback");
                    }
                }
            }
        });

        getBridge().registerHandler("rest_get", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("get got:" + data);
                JSONObject params = null;
                try {
                    params = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RestClient.getInstance().bridgeRequest(params, RestClient.METHOD_GET, callback);
            }
        });

        getBridge().registerHandler("rest_post", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("post got:" + data);

                JSONObject params = null;
                try {
                    params = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RestClient.getInstance().bridgeRequest(params, RestClient.METHOD_POST, callback);
            }
        });

        getBridge().registerHandler("rest_put", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("put got:" + data);

                JSONObject params = null;
                try {
                    params = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RestClient.getInstance().bridgeRequest(params, RestClient.METHOD_PUT, callback);
            }
        });

        getBridge().registerHandler("rest_delete", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("delete got:" + data);

                JSONObject params = null;
                try {
                    params = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RestClient.getInstance().bridgeRequest(params, RestClient.MEHOTD_DELETE, callback);
            }
        });

        // Cache Data set/get - set key/value to data. !!!Do not save preferences
        // cached data will be REMOVED after app uninstalled.
        getBridge().registerHandler("setData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setData got:" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = object.optString("key");
                String value = object.optString("value");

                SharedPreferences.Editor editor = prefs.edit();

                if (null == value || value.equals("null")) {
                    editor.remove(key);
                } else {
                    editor.putString(key, value);
                }
                editor.apply();

                // 如果是设置 userLogin, 则为登陆
                if (Constants.kWebDataKeyUserLogin.equals(key)) {
                    if (mBridgeListener != null) {
                        mBridgeListener.onBridgeSignIn(data);
                    }
                }
            }
        });
        getBridge().registerHandler("getData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getData got:" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = object.optString("key");
                String value = prefs.getString(key, null);

                if (null != callback) {
                    callback.callback(value);
                }
            }
        });

        getBridge().registerHandler("updateData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("updateData got:" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = object.optString("key");
                String value = object.optString("value");

                SharedPreferences.Editor editor = prefs.edit();

                if (null == value || value.equals("null")) {
                    editor.remove(key);
                } else {
                    editor.putString(key, value);
                }

                editor.apply();

                if (null != callback) {
                    callback.callback(value);
                }
            }
        });

        // finish activity by JS code
        getBridge().registerHandler("finishActivity", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("finishActivity" + data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeFinishActivity(data);
                }
            }
        });

        getBridge().registerHandler("openLinkWithBrowser", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("openLinkWithBrowser" + data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeOpenNewLinkWithExternalBrowser(data);
                }
            }
        });

        getBridge().registerHandler("sendNoticeMessage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("sendNoticeMessage" + data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeSendNoticeMessage(data, callback);
                }
            }
        });

        getBridge().registerHandler("chatByUserId", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("chatByUserId" + data);

                JSONObject object;
                try {
                    object = new JSONObject(data);

                    String leanIds = object.optString("lean_id");
                    String[] ids = leanIds.split(",");
                    if (ids == null) {
                        Timber.e("Peer IDs can not be empty");
                    } else if (1 == ids.length) {
                        if (null != mBridgeListener) {
                            mBridgeListener.onBridgeStartSingleChat(object);
                        }
                    } else {
                        if (null != mBridgeListener) {
                            mBridgeListener.onBridgeStartGroupChat(object);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (null != callback) {
                    callback.callback(null);
                }
            }
        });

        getBridge().registerHandler("chatWithCustomerService", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("chatWithCustomerService" + data);

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeStartServiceChat();
                }
            }
        });

        /**
         * Web 调用这个方法，Native 返回给 Web 中介的定位信息和用户首页选择的地址，只要城市就可以
         */
        getBridge().registerHandler("getChosenLocation", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getChosenLocation");

                String city = prefs.getString(Constants.kPrefsLastSelectedCityFromMap, "");
                String district = prefs.getString(Constants.kPrefsLastSelectedDistrictFromMap, "");

                if (TextUtils.isEmpty(city) || TextUtils.isEmpty(district)) {
                    // 如果用户/中介没有选择位置, 则直接返回定位位置
                    BDLocation location = DMApplication.getInstance().getLastActivatedLocation();
                    if (null != location) {
                        city = location.getCity();
                        district = location.getDistrict();
                    }
                }

                JSONObject object = new JSONObject();
                try {
                    object.put("city", city);
                    object.put("district", district);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (null != callback) {
                    callback.callback(object.toString());
                }
            }
        });

        getBridge().registerHandler("getRecentLocation", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getRecentLocation" + data);

                BDLocation location = DMApplication.getInstance().getLastActivatedLocation();
                if (location != null) {
                    JSONObject object = new JSONObject();
                    try {
                        String city = location.getCity();
                        String district = location.getDistrict();
                        object.put("addr", city + district + location.getStreet());
                        object.put("city", city);
                        object.put("district", district);
                        object.put("lat", location.getLatitude());
                        object.put("lng", location.getLongitude());
                        object.put("province", location.getProvince());
                        object.put("street", location.getStreet());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (null != callback) {
                        callback.callback(object);
                    }
                } else {
                    if (null != mBridgeListener) {
                        mBridgeListener.onBridgeRequestLocation(callback);
                    }
                }
            }
        });

        getBridge().registerHandler("cutImage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mCallback = jsCallback;

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeResizeOrCropImage();
                }
            }
        });

        getBridge().registerHandler("broadcast", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(final String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                EventBus.getDefault().post(new WebBroadcastEvent(data, getActivity()));
            }
        });

        getBridge().registerHandler("uploadFiles", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mCallback = jsCallback;

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeUploadFiles(data, jsCallback);
                }
            }
        });

        getBridge().registerHandler("reviewImages", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                com.alibaba.fastjson.JSONArray jsonArray = JSON.parseObject(data).getJSONArray("urls");

                List<String> images = JSON.parseArray(jsonArray.toString(), String.class);
                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeShowImageGallery(images);
                }
            }
        });

        getBridge().registerHandler("showDateTimePicker", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("showDateTimePicker", data);

                throw new NotImplementedException("NOT IMPLEMENTED");
            }
        });

        getBridge().registerHandler("logout", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeLogout();
                }
            }
        });

        getBridge().registerHandler("showPickerView", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("showPickerView");

                // ignore if picker already shown
                if (pickPopWin != null && pickPopWin.isShowing()) return;

                ArrayList item1 = new ArrayList();
                ArrayList item2 = new ArrayList();
                ArrayList item3 = new ArrayList();
                ArrayList selection = new ArrayList();

                try {
                    JSONObject object = new JSONObject(data);

                    JSONArray jsonArray = object.optJSONArray("choices");
                    JSONArray defaultSelection = object.optJSONArray("chosen");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONArray jsonRows = jsonArray.optJSONArray(i);
                        for (int j = 0; j < jsonRows.length(); j++) {
                            if ("undefined".equals(jsonRows.optString(j))) break;

                            if (i == 0) {
                                item1.add(jsonRows.optString(j));
                            } else if (i == 1) {
                                item2.add(jsonRows.optString(j));
                            } else {
                                item3.add(jsonRows.optString(j));
                            }

                            selection.add(defaultSelection.optString(j));
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // ignore if there already has pickers
                pickPopWin = new PickerPopWindow(getActivity(), item1, item2, item3, selection, new PickerPopWindow.OnPickCompletedListener() {
                    @Override
                    public void onAddressPickCompleted(String item1, String item2, String item3) {
                        JSONArray jsonArray = new JSONArray();

                        if (!TextUtils.isEmpty(item1)) jsonArray.put(item1);
                        if (!TextUtils.isEmpty(item2)) jsonArray.put(item2);
                        if (!TextUtils.isEmpty(item3)) jsonArray.put(item3);

                        getBridge().callHandler("didSelectPickerView", jsonArray);
                    }
                });

                pickPopWin.showPopWin(getActivity());
            }
        });

        getBridge().registerHandler("showActionSheet", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("showActionSheet");
                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeShowActionSheet(data, jsCallback);
                }
            }
        });

        getBridge().registerHandler("getRecentChattingList", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("getRecentChattingList");

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeGetRecentChatList(data, jsCallback);
                }
            }
        });

        /**
         * 发送推荐房源消息
         */
        getBridge().registerHandler("sendRecommendedMessage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("sendRecommendedMessage");

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeSendRecommendedMessage(data);
                }
            }
        });

        /**
         * 发送通用卡片消息
         */
        getBridge().registerHandler("sendCardMessage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("sendCardMessage");

                if (null != mBridgeListener) {
                    mBridgeListener.onBridgeSendCardMessage(data);
                }
            }
        });

        /**
         * 按房源显示消息列表
         */
        getBridge().registerHandler("getLastMessageByHouse", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                MessageDao messageDao = DMApplication.getInstance().getMessageDao();

                if (messageDao == null) return;

                // 返回一个这个 house 下边所有用户的最后一条消息的list
                List<com.lfy.bean.Message> list = messageDao.queryBuilder().build().list();

                JSONObject objectList = new JSONObject();

                for (int i = 0; i < list.size(); i++) {
                    com.lfy.bean.Message message = list.get(i);

                    JSONObject object = new JSONObject();
                    try {
                        String msg = message.getMessage();
//                        String modifiedString = EmojiHelper.demojizedTextInWebFormat(msg);

                        object.put("message", msg);
                        object.put("date", message.getDate());
                        object.put("is_read", message.getIs_read());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    JSONObject object1 = new JSONObject();
                    try {
                        object1.put(message.getLeanId(), object);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    JSONObject object2 = new JSONObject();
                    try {
                        object2.put(message.getAuditType(), object1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        objectList.put(message.getHouseId(), object2);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (jsCallback != null) {
                    jsCallback.callback(objectList.toString());
                }
            }
        });

        getBridge().registerHandler("showSearchBar", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                if (getActivity() == null) return;

                EditText searchBar = ButterKnife.findById(getActivity(), R.id.search_bar);
                if (searchBar == null) {
                    searchBar = ButterKnife.findById(getActivity(), R.id.et_search);

                    // 如果找不到搜索框, 退出
                    if (searchBar == null) return;

                    if (null != mBridgeListener) {
                        mBridgeListener.onBridgeShowSearchBar(data);
                    }
                }

                searchBar.setSingleLine();
                searchBar.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

                if (TextUtils.isEmpty(data)) {
                    searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                String content = v.getText().toString();
                                if (!TextUtils.isEmpty(content)) {
                                    getBridge().callHandler("searchContent", content);
                                }
                                return true;
                            }
                            return false;
                        }
                    });
                } else {
                    JSONObject object = null;
                    try {
                        object = new JSONObject(data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String placeholder = object.optString("placeholder");
                    if (!TextUtils.isEmpty(placeholder)) {
                        searchBar.setHint(placeholder);
                    }

                    String type = object.optString("inputOrButton");
                    if ("button".equals(type)) {
                        searchBar.setFocusable(false);
                        searchBar.setClickable(true);

                        searchBar.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 如果是 button 类型的, 则直接点击直接回调 searchContent
                                getBridge().callHandler("searchContent");
                            }
                        });
                    } else {
                        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                    String content = v.getText().toString();
                                    if (!TextUtils.isEmpty(content)) {
                                        getBridge().callHandler("searchContent", content);
                                    }
                                    return true;
                                }
                                return false;
                            }
                        });
                    }
                }
            }
        });

        getBridge().registerHandler("selectImageFromNative", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("selectImageFromNative", data);
                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeSelectImageFromNative(data, jsCallback);
                }
            }
        });

        getBridge().registerHandler("dropToMessage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("dropToMessage");

                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeDropToMessage();
                }
            }
        });

        getBridge().registerHandler("dropToCentre", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("dropToCentre");

                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeDropToCenter();
                }
            }
        });

        getBridge().registerHandler("updateFriendRelationship", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("updateFriendRelationship");

                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeUpdateFriendRelationship();
                }
            }
        });

        getBridge().registerHandler("selectMapLocation", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("selectMapLocation");

                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeSelectMapLocation(data);
                }
            }
        });

        getBridge().registerHandler("updateData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("updateData");

                if (mBridgeListener != null) {
                    mBridgeListener.onBridgeUpdateUserMessage(data);
                }
            }
        });

    }

    public void onEvent(BridgeCallbackEvent event) {
        // 加一个参数 result.refreshAll = true | false;
        try {
            JSONObject object = new JSONObject(event.getHolder());
            object.put("refreshAll", true);

            getBridge().callHandler("onLastMessageChangeByHouse", object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open new url in webview
     *
     * @param url destination file url without base url
     */
    public void redirectToLoadUrl(String url, HashMap<String, String> additionalHttpHeaders) {
        if (mWebView == null) {
            mUrl = url;
            this.additionalHttpHeaders = additionalHttpHeaders;
            return;
        }

        String rootPagesFolder = DMApplication.getInstance().getRootPagesFolder();

        if (StringUtil.isValidHTTPUrl(url)) {
            mUrl = url;
        } else {
            mUrl = rootPagesFolder + "/" + url;
        }

        Timber.i("URL - " + mUrl);

        if (StringUtil.isValidHTTPUrl(mUrl)) {
            // url is web link
            if (additionalHttpHeaders != null) {
                mWebView.loadUrl(mUrl, additionalHttpHeaders);
            } else {
                mWebView.loadUrl(mUrl);
            }
        } else {
            mWebView.loadUrl("file:///" + mUrl);
        }
    }

    public void redirectToLoadUrl(String url) {
        redirectToLoadUrl(url, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        KeyboardUtils.hideSoftInput(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();

        KeyboardUtils.hideSoftInput(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentListener = (OnFragmentInteractionListener) activity;
            mBridgeListener = (OnBridgeInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mFragmentListener = null;
        mBridgeListener = null;
    }

    private void hideIME() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mWebView.getWindowToken(), 0);
    }

    @Override
    public void OnBridgeWebViewPageStop() {
        if (getActivity() != null) {
            ((WebViewBasedActivity) getActivity()).onBridgeDismissProgressDialog();
        }
    }

    @Override
    public void OnBridgeWebViewPageStart() {
        if (getActivity() != null) {
            ((WebViewBasedActivity) getActivity()).onBridgeShowProgressDialog();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(WebViewBaseFragment fragment);
    }

    public interface OnBridgeInteractionListener {
        void onBridgeRequestPurchase(String data, WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeSelectImageFromNative(String data, WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeOpenNewLink(String url, JSONObject params);

        void onBridgeReplaceLink(String url, JSONObject object);

        void onBridgeShowSearchBar(String object);

        void onBridgeLogout();

        void onBridgeShowProgressDialog();

        void onBridgeDismissProgressDialog();

        void onBridgeSetTitle(String title);

        void onBridgeSetRightItem(int resourceId, View.OnClickListener onRightItemClick);

        void onBridgeSetRightItem(String text, View.OnClickListener onRightItemClick);

        void onBridgeUploadFiles(String datum, WebViewJavascriptBridge.WVJBResponseCallback jsCallback);

        void onBridgeResizeOrCropImage();

        void onBridgeFinishActivity(String data);

        void onBridgeRequestLocation(WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeOpenNewLinkWithExternalBrowser(String data);

        void onBridgeUpdateFriendRelationship();

        void onBridgeDropToMessage();

        void onBridgeSignIn(String data);

        void onBridgeSelectMapLocation(String data);

        void onBridgeShowImageGallery(List<String> data);

        void onBridgeSendNoticeMessage(String data, WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeShowActionSheet(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback);

        void onBridgeGetRecentChatList(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback);

        void onBridgeShowErrorMessage(JSONObject data);

        void onBridgeSendRecommendedMessage(String data);

        void onBridgeSendCardMessage(String data);

        void onBridgeUpdateUserMessage(String data);

        void onBridgeHideRightItem();

        void onBridgeStartGroupChat(JSONObject object);

        void onBridgeStartSingleChat(JSONObject object);

        void onBridgeStartServiceChat();

        void onBridgeDropToCenter();
    }
}
