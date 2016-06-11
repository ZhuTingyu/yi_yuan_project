package com.yuan.house.ui.fragment;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.SaveCallback;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.chat.ui.chat.ChatRoomActivity;
import com.baidu.location.BDLocation;
import com.dimo.http.RestClient;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.lfy.dao.MessageDao;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.yuan.house.activities.ImagePagerActivity;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.event.WebBroadcastEvent;
import com.yuan.house.ui.view.PickerPopWindow;
import com.yuan.house.utils.ToastUtil;
import com.yuan.house.R;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/13/15.
 */
public class WebViewBaseFragment extends Fragment {
    protected WebViewJavascriptBridge bridge;
    protected OnFragmentInteractionListener mFragmentListener;
    protected OnBridgeInteractionListener mBridgeListener;
    protected String mUrl;
    @Inject
    protected SharedPreferences prefs;
    WebViewJavascriptBridge.WVJBResponseCallback mCallback;
    @InjectView(R.id.webview)
    WebView mWebView;
    HashMap<String, String> additionalHttpHeaders;
    private Calendar calendar;

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
    }

    @Override
    public void onDestroyView() {
        WebStorage.getInstance().deleteAllData();
        ViewGroup holder = ButterKnife.findById(getActivity(), R.id.webview_parent);
        if (holder != null) {
            holder.removeView(mWebView);
        }
        mWebView.removeAllViews();
        mWebView.destroy();

        super.onDestroyView();

        ButterKnife.reset(this);
    }

    public View createView(LayoutInflater inflater, int resId, ViewGroup container, Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        View view = inflater.inflate(resId, container, false);

        Injector.inject(this);

        ButterKnife.inject(this, view);

        Timber.v("onCreateView");

        if (mFragmentListener != null) {
            bridge = new WebViewJavascriptBridge(getActivity(), mWebView, null);

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

        // register all the web handlers at once
        registerHandle();

        bridge.registerHandler("getParams", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getParams got:" + data);

                HashMap<String, String> map = getAdditionalHttpHeaders();

                JSONObject object = null;

                if (map != null) {
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
        bridge.registerHandler("purchase", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Log.i("reponse.data", data);

                mBridgeListener.onBridgeRequestPurchase(callback);
            }
        });

        bridge.registerHandler("redirectPage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("redirectPage got:" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = params.get("url");
                mBridgeListener.onBridgeOpenNewLink(url, params);

                if (null != callback) {
                    callback.callback("redirectPage answer");
                }
            }
        });

        // This will NOT working since the webview might be not ready!!!
        // bridge.callHandler("testJavascriptHandler", "42");
        bridge.registerHandler("replacePage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("replacePage got:" + data);
                if (null != callback) {
                    callback.callback("replacePage answer");
                }
            }
        });

        bridge.registerHandler("showToast", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                ToastUtil.showShort(getActivity(), data);
            }
        });

        bridge.registerHandler("showConfirmDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showConfirmDialog got:" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final NiftyDialogBuilder dialogBuilder = NiftyDialogBuilder.getInstance(getActivity());
                dialogBuilder.withMessage(params.get("msg"))
                        .withDialogColor("#FFE74C3C")
                        .withEffect(Effectstype.SlideBottom)
                        .withButton1Text(getString(R.string.confirm))
                        .withButton2Text(getString(R.string.cancel))
                        .withDuration(300)
                        .setButton1Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialogBuilder.dismiss();
                            }
                        })
                        .setButton2Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialogBuilder.dismiss();
                            }
                        })
                        .show();

            }
        });

        bridge.registerHandler("showMsg", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showMsg got:" + data);
                if (null != callback) {
                    callback.callback("showMsg answer");
                }
            }
        });

        bridge.registerHandler("showProgressDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showProgressDialog got:" + data);

                mBridgeListener.onBridgeShowProgressDialog();
            }
        });

        bridge.registerHandler("dismissProgressDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("dismissProgressDialog got:" + data);

                mBridgeListener.onBridgeDismissProgressDialog();
            }
        });

        bridge.registerHandler("updatePackage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("updatePackage got:" + data);
                if (null != callback) {
                    callback.callback("updatePackage answer");
                }
            }
        });

        bridge.registerHandler("uploadFile", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("uploadFile got:" + data);

                if (null != callback) {
                    HashMap<String, String> stringStringHashMap = null;
                    try {
                        stringStringHashMap = StringUtil.JSONString2HashMap(data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String filePath = stringStringHashMap.get("fileUrl");

                    File file = new File(filePath);
                    RequestParams params = new RequestParams();
                    try {
                        params.put("image", file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    //TODO: update file url
                    RestClient.getInstance().post(null, params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Timber.v("Success");

                            //TODO: upload file
                            callback.callback(null);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Timber.e("Failed");
                        }
                    });
                }
            }
        });

        bridge.registerHandler("getCurrentPackageVersion", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getCurrentPackageVersion got:" + data);
                if (null != callback) {
                    String version = prefs.getString(Constants.kApplicationPackageVersion, "100");

                    callback.callback(version);
                }
            }
        });
        bridge.registerHandler("setTitle", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setTitle got:" + data);
                HashMap<String, String> stringStringHashMap = null;
                try {
                    stringStringHashMap = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String title = stringStringHashMap.get("text");

                mBridgeListener.onBridgeSetTitle(title);
            }
        });

        bridge.registerHandler("setRightItem", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setRightItem got:" + data);
                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final String text = params.get("type");
                final String content = params.get("content").toString();
                if (!TextUtils.isEmpty(text) && text.equals("icon")) {
                    Resources resources = getResources();
                    String icon = content.substring(0, params.get("content").indexOf("."));
                    int resourceId = resources.getIdentifier(icon, "drawable", getActivity().getPackageName());
                    mBridgeListener.onBridgeSetRightItem(resourceId, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bridge.callHandler("onRightItemClick");
                        }
                    });
                } else {
                    // show a text button
                    mBridgeListener.onBridgeSetRightItem(content, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bridge.callHandler("onRightItemClick", content, null);
                        }
                    });
                }
            }
        });

        bridge.registerHandler("getAOSPVersion", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getAOSPVersion got:" + data);
                if (null != callback) {
                    callback.callback(Build.VERSION.RELEASE);
                }
            }
        });

        bridge.registerHandler("callNumber", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("callNumber got:" + data);

                HashMap<String, String> stringStringHashMap = null;
                try {
                    stringStringHashMap = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (stringStringHashMap != null) {
                    final String phone = stringStringHashMap.get("phone");
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

//        bridge.registerHandler("showTabbar", new WebViewJavascriptBridge.WVJBHandler() {
//            @Override
//            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
//                Timber.v("showTabbar got:" + data);
//
//                HashMap<String, String> stringStringHashMap = null;
//                try {
//                    stringStringHashMap = StringUtil.JSONString2HashMap(data);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//                if (null != callback) {
//                    View view = ButterKnife.findById(MainActivity.this, R.id.tabbar_layout);
//                    if (view != null) {
//                        String str = stringStringHashMap.get("visible");
//                        if (str.equals("false")) {
//                            view.setVisibility(View.GONE);
//                        } else {
//                            view.setVisibility(View.VISIBLE);
//                        }
//                    }
//                    callback.callback(null);
//                }
//            }
//        });

        bridge.registerHandler("rest_get", new WebViewJavascriptBridge.WVJBHandler() {
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

        bridge.registerHandler("rest_post", new WebViewJavascriptBridge.WVJBHandler() {
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

        bridge.registerHandler("rest_put", new WebViewJavascriptBridge.WVJBHandler() {
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

        bridge.registerHandler("rest_delete", new WebViewJavascriptBridge.WVJBHandler() {
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

        bridge.registerHandler("login", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("login got:" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String avId = prefs.getString("AVInstallationId", "");

                // convert HashMap to RequestParams
                RequestParams requestParams = new RequestParams();
                requestParams.put("product", "KidsParentAPK");
                requestParams.put("username", params.get("username"));
                requestParams.put("password", params.get("password"));
                requestParams.put("installationid", avId);

                if (null != callback) {
                    RestClient.getInstance().get(Constants.kServiceLogin, null, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            super.onSuccess(statusCode, headers, response);

                            if (response != null) {
                                try {
                                    response.put("statusCode", statusCode);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.callback(response.toString());
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            super.onFailure(statusCode, headers, throwable, errorResponse);

                            if (errorResponse != null) {
                                try {
                                    errorResponse.put("statusCode", statusCode);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            callback.callback(errorResponse.toString());
                        }
                    });
                }
            }
        });

        // Preference set/get - set key/value to share preference. !!!Do not save big data
        // Preference will be permanant even if the app is uninstalled.
        bridge.registerHandler("setPref", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setPref got:" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = params.get("key");
                String value = params.get("value");

                prefs.edit().putString(key, value).apply();

                if (null != callback) {
                    callback.callback(null);
                }
            }
        });
        bridge.registerHandler("getPref", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getPref got:" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = params.get("key");
                String value = prefs.getString(key, "");

                JSONObject object = new JSONObject();
                try {
                    object.put("key", key);
                    object.put("value", value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (null != callback) {
                    callback.callback(object.toString());
                }
            }
        });

        // Cache Data set/get - set key/value to data. !!!Do not save preferences
        // cached data will be REMOVED after app uninstalled.
        bridge.registerHandler("setData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setData got:" + data);

                HashMap<String, String> params;
                try {
                    params = StringUtil.JSONString2HashMap(data);

                    String key = params.get("key");
                    String value = params.get("value");

                    SharedPreferences.Editor editor = prefs.edit();
                    // TODO: 16/6/5 如果是设置 userLogin,则为登陆
                    if (Constants.kWebDataKeyUserLogin.equals(key)) {
                        mBridgeListener.onBridgeSignIn(data);

                        return;
                    }

                    if (value == null || value.equals("null")) {
                        editor.remove(key);
                    } else {
                        editor.putString(key, value);
                    }
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        bridge.registerHandler("getData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getData got:" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = params.get("key");
                String value = prefs.getString(key, null);

                if (null != callback) {
                    callback.callback(value);
                }
            }
        });

        // finish activity by JS code
        bridge.registerHandler("finishActivity", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("finishActivity" + data);

                mBridgeListener.onBridgeFinishActivity(data);
            }
        });

        bridge.registerHandler("openLinkWithBrowser", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("openLinkWithBrowser" + data);

                mBridgeListener.onBridgeOpenNewLinkWithExternalBrowser(data);
            }
        });

        bridge.registerHandler("chatByUserId", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("chatByUserId" + data);

                JSONObject object = null;
                try {
                    object = new JSONObject(data);

                    /** Start Chat **/
                    ChatRoomActivity.chatByUserId(getActivity(), object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }



                if (null != callback) {
                    callback.callback(null);
                }
            }
        });

        bridge.registerHandler("followByUserId", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("followByUserId" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String objectId = params.get("objectId");

//                ChatRoomActivity.chatByUserId(WebViewBasedActivity.this, objectId);
                UserService.addFriend(objectId, new SaveCallback() {
                    @Override
                    public void done(AVException e) {
                        if (null != callback) {
                            callback.callback(e == null ? null : e.getMessage());
                        }
                    }
                });
            }
        });

        bridge.registerHandler("getFollowees", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getFollowees " + data);
                try {
                    List<AVUser> list = UserService.findFriends(new FindCallback<AVUser>() {
                        @Override
                        public void done(List<AVUser> list, AVException e) {
                            JSONArray ret;
                            try {
                                ret = new JSONArray();
                                for (AVUser user : list) {
                                    JSONObject item = new JSONObject();
                                    item.put("nickname", user.getUsername());
                                    item.put("chat_user_id", user.getObjectId());
                                    ret.put(item);
                                }
                            } catch (Exception e1) {
                                ret = null;
                            }
                            if (callback != null) {
                                callback.callback(ret == null ? "[]" : ret.toString());
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /**
         * Invoke LeanChat login stuff
         */
        bridge.registerHandler("reverseLeanChatLogin", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("reverseLeanChatLogin" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String username = params.get("user");
                String passwd = params.get("passwd");


                if (null != callback) {
                    callback.callback(null);
                }
            }
        });

        /**
         * get geo location
         */
        bridge.registerHandler("getGeoLocation", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getGeoLocation" + data);

                BDLocation location = DMApplication.getInstance().getLastActivatedLocation();
                if (location != null) {
                    JSONObject object = new JSONObject();
                    try {
                        object.put("addr", location.getAddrStr());
                        object.put("city", location.getCity());
                        object.put("district", location.getDistrict());
                        object.put("lat", location.getLatitude());
                        object.put("lng", location.getLongitude());
                        object.put("province", location.getProvince());
                        object.put("street", location.getStreet());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (callback != null) {
                        callback.callback(object.toString());
                    }
                } else {
                    mBridgeListener.onBridgeRequestLocation(callback);
                }
            }
        });

        bridge.registerHandler("cutImage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mCallback = jsCallback;

                mBridgeListener.onBridgeResizeOrCropImage();
            }
        });

        bridge.registerHandler("broadcast", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(final String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                EventBus.getDefault().post(new WebBroadcastEvent(data, getActivity()));
            }
        });

        //TODO 代码已完善，待测试。
        bridge.registerHandler("uploadFiles", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mCallback = jsCallback;

                mBridgeListener.onBridgeUploadFiles();
            }
        });

        bridge.registerHandler("reviewImages", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("图片预览", data);
                Intent intent = new Intent(getActivity(), ImagePagerActivity.class);
                intent.putExtra("json", data);
                startActivity(intent);
            }
        });

        bridge.registerHandler("showDateTimePicker", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("showDateTimePicker", data);
                mCallback = jsCallback;
                try {
                    HashMap<String, String> map = StringUtil.JSONString2HashMap(data);
                    String date = map.get("pick_date");
                    calendar = Calendar.getInstance();
                    if (date.equals("true")) {
                        //选择日期
//                        DatePickerDialog.newInstance(new WebViewBasedActivity.DataPickerOnClickListener(), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show(mFragmentManager, "datePicker");
                    } else {
                        //选择时间
//                        TimePickerDialog.newInstance(new WebViewBasedActivity.TimePickerOnClickListener(), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(mFragmentManager, "timePicker");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        bridge.registerHandler("logout", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mBridgeListener.onBridgeLogout();
            }
        });

        bridge.registerHandler("showPickerView", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("showPickerView", data);

                ArrayList item1 = new ArrayList();
                ArrayList item2 = new ArrayList();
                ArrayList item3 = new ArrayList();
                try {
                    JSONArray jsonArray = new JSONArray(data);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.opt(i);
                        JSONArray jsonRows = (JSONArray) jsonObject.get("rows");
                        for (int j = 0; j < jsonRows.length(); j++) {
                            if (i == 0)
                                item1.add(jsonRows.opt(j).toString());
                            else if (i == 1)
                                item2.add(jsonRows.opt(j).toString());
                            else
                                item3.add(jsonRows.opt(j).toString());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                PickerPopWindow pickPopWin = new PickerPopWindow(getActivity(), item1, item2, item3, new PickerPopWindow.OnPickCompletedListener() {
                    @Override
                    public void onAddressPickCompleted(String item1, String item2, String item3) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(item1);
                        sb.append(item2);
                        sb.append(item3);
                        Log.i("result", sb.toString());
                    }
                });

                pickPopWin.showPopWin(getActivity());

            }
        });

        bridge.registerHandler("getLastMessageByHouse", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                MessageDao messageDao = DMApplication.getInstance().getMessageDao();
                List<com.lfy.bean.Message> list = messageDao.queryBuilder().build().list();
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < list.size(); i++) {
                    com.lfy.bean.Message message = list.get(i);
                    sb.append("{");
                    sb.append(prefs.getString("userId", null) + ":");
                    sb.append("{");
                    sb.append(message.getHouseId() + ":");
                    sb.append("{");
                    sb.append(message.getAuditType() + ":");
                    sb.append("{");
                    sb.append(message.getLeanId() + ":");
                    sb.append("{\"message\" : " + message.getDate() + ", \"is_read\" : " + message.getIs_read() + "}");
                    sb.append("}");
                    sb.append("}");
                    sb.append("}");
                    sb.append("}");
                    if (list.size() - 1 != i)
                        sb.append(",");
                }

                jsCallback.callback(sb.toString());
            }
        });

        bridge.registerHandler("showSearchBar", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
//                setTitleSearch();
                mBridgeListener.onBridgeShowSearchBar();
            }
        });

        bridge.registerHandler("selectImageFromNative", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("selectImageFromNative", data);

                mBridgeListener.onBridgeSelectImageFromNative(data, jsCallback);
            }
        });

        bridge.registerHandler("dropToMessage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mBridgeListener.onBridgeDropToMessage();
            }
        });

        bridge.registerHandler("updateFriendRelationship", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mBridgeListener.onBridgeUpdateFriendRelationship();
            }
        });

        bridge.registerHandler("selectMapLocation", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mBridgeListener.onBridgeSelectMapLocation();
            }
        });

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
        void onBridgeRequestPurchase(WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeSelectImageFromNative(String data, WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeOpenNewLink(String url, HashMap<String, String> params);

        void onBridgeShowSearchBar();

        void onBridgeLogout();

        void onBridgeShowProgressDialog();

        void onBridgeSetTitle(String title);

        void onBridgeSetRightItem(int resourceId, View.OnClickListener onRightItemClick);

        void onBridgeSetRightItem(String text, View.OnClickListener onRightItemClick);

        void onBridgeUploadFiles();

        void onBridgeResizeOrCropImage();

        void onBridgeDismissProgressDialog();

        void onBridgeFinishActivity(String data);

        void onBridgeRequestLocation(WebViewJavascriptBridge.WVJBResponseCallback callback);

        void onBridgeOpenNewLinkWithExternalBrowser(String data);

        void onBridgeUpdateFriendRelationship();

        void onBridgeDropToMessage();

        void onBridgeSignIn(String data);

        void onBridgeSelectMapLocation();
    }
}
