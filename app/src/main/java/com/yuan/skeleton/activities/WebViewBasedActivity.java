package com.yuan.skeleton.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.SaveCallback;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.chat.ui.chat.ChatRoomActivity;
import com.avoscloud.chat.util.Utils;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.http.RestClient;
import com.dimo.utils.DeviceUtil;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.yuan.skeleton.R;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.base.BaseFragmentActivity;
import com.yuan.skeleton.common.Constants;
import com.yuan.skeleton.ui.fragment.WebViewBaseFragment;
import com.yuan.skeleton.ui.fragment.WebViewFragment;
import com.victor.loading.rotate.RotateLoading;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/12/15.
 */
public class WebViewBasedActivity extends BaseFragmentActivity implements WebViewFragment.OnFragmentInteractionListener {
    public static final int kActivityRequestCodeCamera = 1;
    public static final int kActivityRequestCodeGallery = 2;
    public static final int kActivityRequestCodeWebActivity = 3;
    public static final int kActivityRequestCodeSelectMapLocation = 4;
    public static final int kActivityRequestCodeEditText = 5;
    public static final int kActivityRequestCodeDateTimePicker = 6;
    public static final int kActivityRequestCodeDialog = 7;
    public WebViewJavascriptBridge bridge;
    protected FragmentManager mFragmentManager;
    protected FragmentTransaction mFragmentTransaction;
    protected WebView mWebView;
    protected String mUrl;

    @InjectView(R.id.rotateloading)
    protected RotateLoading mLoadingDialog;

    WebViewJavascriptBridge.WVJBResponseCallback mCallback;
    Uri fileUri;
    JSONArray mImgResizeCfg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.inject(this);

        mContext = this;

        // prepare the fragment for main framelayout
        mFragmentManager = getSupportFragmentManager();

        HashMap<String, String> params = null;
        String url = Constants.kWebPageEntry;

        Bundle bundle = getIntent().getExtras();

        if (!TextUtils.isEmpty(url)) {
            setContentView(R.layout.activity_webview, true);
        } else {
            setContentView(R.layout.activity_webview);
        }

        if (bundle != null) {
            params = (HashMap<String, String>) bundle.getSerializable("params");
            if (params != null) {
                try {
                    mUrl = bundle.getString("url");

                    JSONObject object = new JSONObject(params.get("params"));

                    if (!TextUtils.isEmpty(object.optString("title"))) {
                        setTitleItem(object.optString("title"));
                    }

                    if (!TextUtils.isEmpty(object.optString("hasBackButton"))) {
                        if (object.optString("hasBackButton").equals("true")) {
                            setLeftItem(R.mipmap.ic_back, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Timber.v("OnClick back button");
                                    finish();
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        ButterKnife.inject(this);

        // Register event bus to receive events
//        EventBus.getDefault().register(this);

        if (mFragmentTransaction == null) {
            mFragmentTransaction = mFragmentManager.beginTransaction();
            mFragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
    }

    protected Fragment getFragment(String tag) {
        Fragment f = mFragmentManager.findFragmentByTag(tag);

        if (f != null) {
            Timber.i("Found Fragment : " + tag);

            return f;
        }

        if (tag.equals(Constants.kFragmentTagWebView)) {
            f = WebViewFragment.newInstance();
        } else {

        }

        Timber.v("NOT Found Fragment : " + tag + ", need to create!!!");

        return f;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void registerHandle() {
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
                openLinkInNewActivity(url, params);

                if (null != callback) {
                    callback.callback("redirectPage answer");
                }

//                // FIXME: make sure you use bridge.callHandler in a WVJBHandler loop
//                bridge.callHandler("testJavascriptHandler", "42");
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

                final NiftyDialogBuilder dialogBuilder = NiftyDialogBuilder.getInstance(mContext);
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

                mLoadingDialog.start();
            }
        });

        bridge.registerHandler("dismissProgressDialog", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("dismissProgressDialog got:" + data);

                if (mLoadingDialog != null && mLoadingDialog.isStart())
                    mLoadingDialog.stop();

                if (null != callback) {
                    callback.callback("dismissProgressDialog answer");
                }
            }
        });

        bridge.registerHandler("setUnreadMsgNumber", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setUnreadMsgNumber got:" + data);

                HashMap<String, String> stringStringHashMap = null;
                try {
                    stringStringHashMap = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (null != callback) {
                    if (callback != null) {
                        try {
                            callback.getClass().getMethod("callback", String.class);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        callback.callback(null);
                    }
                }
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

        bridge.registerHandler("selectPhoto", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("selectPhoto got:" + data);
                mImgResizeCfg = null;
                try {
                    JSONObject params = new JSONObject(data);
                    mImgResizeCfg = params.getJSONArray("resize");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCallback = callback;

                final NiftyDialogBuilder dialogBuilder = NiftyDialogBuilder.getInstance(mContext);
                dialogBuilder.withTitle(getString(R.string.title_get_photo))
                        .withTitleColor("#FFFFFF")
                        .withMessage(R.string.use_camera_or_gallery)
                        .withDuration(300)
                        .withEffect(Effectstype.SlideBottom)
                        .withButton1Text(getString(R.string.use_camera))
                        .withButton2Text(getString(R.string.use_gallery))
                        .setButton1Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityCamera();
                                dialogBuilder.dismiss();
                            }
                        })
                        .setButton2Click(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivityGallery();
                                dialogBuilder.dismiss();
                            }
                        })
                        .show();
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

                setTitleItem(title);

                if (null != callback) {
                    callback.callback(null);
                }
            }
        });

        bridge.registerHandler("setRightItem", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setRightItem got:" + data);

                HashMap<String, String> stringStringHashMap = null;
                try {
                    stringStringHashMap = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final String text = stringStringHashMap.get("text");

                JSONArray menu = null;
                try {
                    menu = new JSONArray(stringStringHashMap.get("menu"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (TextUtils.isEmpty(text) && menu != null && menu.length() != 0) {
                    // show a '...' image button
                    final JSONArray finalMenu = menu;
                    setRightItem(R.drawable.ic_more, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // show a dropdown menu based on the list
                            ImageButton button = ButterKnife.findById(mTopBar, R.id.topbar_right_btn);

                            PopupMenu popupMenu = new PopupMenu(mContext, button);

                            for (int i = 0; i < finalMenu.length(); i++) {
                                try {
                                    String item = finalMenu.getString(i);
                                    popupMenu.getMenu().add(item);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            popupMenu.show();

                            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    Timber.v("onMenuItemClick title : " + item.getTitle().toString());
                                    bridge.callHandler("rightItemClick", item.getTitle().toString(), null);
                                    return false;
                                }
                            });
                            if (null != callback) {
                                callback.callback("right image button clicked");
                            }
                        }
                    });
                } else {
                    // show a text button
                    setRightItem(text, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bridge.callHandler("rightItemClick", text, null);
                            if (null != callback) {
                                bridge.callHandler("rightItemClick", text, null);
                                callback.callback("right text button clicked");
                            }
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

                prefs.edit().putString(key, value).commit();

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

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String key = params.get("key");
                String value = params.get("value");

//TODO: impl
                if (value == null)
                    prefs.edit().remove(key).commit();
                else
                    prefs.edit().putString(key, value).commit();

                if (null != callback) {
                    callback.callback(null);
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

//TODO: impl
                String key = params.get("key");
                String value = prefs.getString(key, null);

                //FIXME: delete this
//                if(key.equals("userLogin")) {
//                    value = "{\n" +
//                            "    \"username\": \"13212345678\",\n" +
//                            "    \"social\": 0,\n" +
//                            "    \"age\": 21,\n" +
//                            "    \"nickname\": \"巴神\",\n" +
//                            "    \"gender\": 1,\n" +
//                            "    \"birthdate\": \"2014-01-23\",\n" +
//                            "    \"recent_geo_latitude\": 78.12,\n" +
//                            "    \"recent_geo_longitude\": 121.21,\n" +
//                            "    \"recent_activated_timestamp\": \"2014-12-01 23:11:05\",\n" +
//                            "    \"faverite_locations\": [{\"lat\":79,\"lng\":110,\"text\":\"九眼桥桥上\"},{\"lat\":79,\"lng\":110,\"text\":\"环球中心\"}],\n" +
//                            "    \"rate_average\": 34,\n" +
//                            "    \"rate_times\": 20,\n" +
//                            "    \"images\": [\"https://file2.teambition.com/site_media/cache/51/7f/4d00af7534bdf44bbeff03d6a1657f51_250x250.jpg\",\"https://file2.teambition.com/site_media/cache/51/7f/4d00af7534bdf44bbeff03d6a1657f51_250x250.jpg\"],\n" +
//                            "    \"city\": \"成都\",\n" +
//                            "    \"slogan\": \"就是一个说说签名\",\n" +
//                            "    \"chat_service_id\": null,\n" +
//                            "    \"chat_service_passwd\": null,\n" +
//                            "    \"access_key\": \"test1\",\n" +
//                            "    \"access_secret\": \"3123213132131\"\n" +
//                            "}";
//                }

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

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String result = params.get("result");

                Bundle conData = new Bundle();
                conData.putString("param_result_after_activity_finished", result);

                Intent intent = new Intent();
                intent.putExtras(conData);
                setResult(RESULT_OK, intent);

                finish();
            }
        });

        bridge.registerHandler("openLinkWithBrowser", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("openLinkWithBrowser" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String link = params.get("link");
                if (!TextUtils.isEmpty(link)) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                }

                if (null != callback) {
                    callback.callback(null);
                }
            }
        });

        bridge.registerHandler("chatByUserId", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("chatByUserId" + data);

                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String objectId = params.get("objectId");

                ChatRoomActivity.chatByUserId(WebViewBasedActivity.this, objectId);

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
                            }catch (Exception e1) {
                                ret = null;
                            }
                            if(callback!=null) {
                                callback.callback(ret==null?"[]":ret.toString());
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

                //renew location client
                final LocationClient locationClient = new LocationClient(getApplicationContext());
                locationClient.setDebug(true);
                LocationClientOption option = new LocationClientOption();
                option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
                option.setScanSpan(1000);
                option.setCoorType("bd09ll");
                option.setIsNeedAddress(true);
                locationClient.setLocOption(option);

                BDLocationListener listener = new BDLocationListener() {
                    @Override
                    public void onReceiveLocation(BDLocation location) {
                        JSONObject obj;
                        locationClient.stop();
                        try {
                            obj = new JSONObject();
                            obj.put("lng", location.getLongitude());
                            obj.put("lat", location.getLatitude());
                            obj.put("addr", location.getAddrStr());
                            obj.put("success", true);
                        } catch (Exception e) {
                            obj = null;
                            Timber.e(e, "get geo location from baidu failed");
                        }
                        if (callback != null) {
                            if (obj == null) {
                                callback.callback("{\"success\":false");
                            } else {
                                callback.callback(obj.toString());
                            }
                        }
                    }
                };
                locationClient.registerLocationListener(listener);
                locationClient.start();
            }
        });
    }

    /**
     * Invoke from JS script interaction
     *
     * @param url    destination url
     * @param params params for page
     */
    public void openLinkInNewActivity(String url, HashMap<String, String> params) {
        Bundle extras = new Bundle();
        extras.putSerializable("params", params);

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("params", params);
        intent.putExtra("url", url);

        startActivityForResult(intent, kActivityRequestCodeWebActivity);
    }

    @Override
    public void onFragmentInteraction(final WebViewBaseFragment fragment) {
        Timber.v("Fragment ready to use");

        mWebView = fragment.getWebView();

        bridge = new WebViewJavascriptBridge(this, mWebView, null);
        bridge.registerHandler("getParams", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("getParams got:" + data);

                HashMap<String, String> map = fragment.getAdditionalHttpHeaders();

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

        registerHandle();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.v("REQUEST CODE - " + requestCode);

        if (requestCode == kActivityRequestCodeCamera) {
            // return from camera
            if (resultCode == RESULT_OK) {
                if (fileUri != null) {
                    mCallback.callback(fileUri.toString());
                }
            } else if (resultCode == RESULT_CANCELED) {
                Timber.w("camera cancelled");
            } else {
                Timber.e("CAMERA - SHOULD NEVER REACH");
            }
        } else if (requestCode == kActivityRequestCodeGallery) {
            // return from gallery
            if (resultCode == RESULT_OK) {
                fileUri = data.getData();
                if (fileUri != null) {
                    String path = Utils.getRealPathFromURI(this, fileUri);
                    Timber.v("IMAGE : " + path);

                    InputStream is;
                    try {
                        is = new FileInputStream(path);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    ByteArrayOutputStream bout = new ByteArrayOutputStream(1024 * 300);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 30, bout);
                    uploadImage(new ByteArrayInputStream(bout.toByteArray()), mImgResizeCfg);
                }
            } else if (resultCode == RESULT_CANCELED) {
                Timber.w("gallery cancelled");
            } else {
                Timber.e("GALLERY - SHOULD NEVER REACH");
            }
        } else if (requestCode == kActivityRequestCodeWebActivity) {
            String result = null;

            if (data != null) {
                // handle the case if activity is terminated by JS code
                Bundle res = data.getExtras();
                result = res.getString("param_result_after_activity_finished");

            }
            Timber.v("Got finished result:" + result);

            // send back the result to original webview
            bridge.callHandler("activityFinished", result);

            return;
        } else if (requestCode == kActivityRequestCodeSelectMapLocation) {
            if (resultCode == 1) {
                JSONObject response = new JSONObject();
                try {
                    response.put("lng", data.getDoubleExtra("lng", 0));
                    response.put("lat", data.getDoubleExtra("lat", 0));
                    response.put("addr", data.getStringExtra("addr"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                bridge.callHandler("onSelectMapLocationComplete", response.toString());
            }
        } else if (requestCode == kActivityRequestCodeEditText) {
            if (resultCode == 1) {
                // TODO: what should I do if get the result from edit text page.
            }
        } else if (requestCode == kActivityRequestCodeDateTimePicker) {
            if (resultCode == 1) {
                bridge.callHandler("onDateTimePicked", String.valueOf(data.getLongExtra("timestamp", 0)));
            }
        } else {
            // never reach
            Timber.e("onActivityResult SHOULD NEVER REACH");
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    //FIXME: use updated photo selection widget
    public void startActivityGallery() {

    }

    public void startActivityCamera() {

    }

    public void uploadImage(InputStream is, JSONArray imgResizeCfg) {
        RequestParams params = new RequestParams();
        // TODO: check data post handler
        params.put("file", is, "image.jpeg");
        try {
            if (imgResizeCfg != null) {
                for (int i = 0; i < imgResizeCfg.length(); i++) {
                    JSONArray cfg = imgResizeCfg.getJSONArray(i);
                    int width = cfg.getInt(0);
                    int height = cfg.getInt(1);
                    params.add("width[]", String.valueOf(width));
                    params.add("height[]", String.valueOf(height));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String accessKey = "";
        try {
//            accessKey = new JSONObject(prefs.getString("userLogin","")).getString("access_key");
            accessKey = "test1";
        } catch (Exception e) {
            Timber.e(e, "not log in");
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("access-key", accessKey);
        client.setResponseTimeout(60);

        client.post(Constants.kServiceHost + Constants.kServiceUploadImage, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Timber.v("onSuccess when post image file");
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    if (response != null) {
                        ret.put("data", response);
                    }
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCallback.callback(ret.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                Timber.e("onFailure when post image file");
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    if (response != null) {
                        ret.put("data", response);
                    }
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCallback.callback(ret.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String response, Throwable throwable) {
                Timber.e("onFailure when post image file, unknown exception");
                Timber.e(response);
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    ret.put("data", new JSONObject("{\"msg\":\"unknown exception\",\"error_code\":0}"));
                    ret.put("error_code", 0);
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCallback.callback(ret.toString());
            }
        });
    }
}
