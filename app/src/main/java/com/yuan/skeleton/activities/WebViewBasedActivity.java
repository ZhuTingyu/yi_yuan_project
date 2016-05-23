package com.yuan.skeleton.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.LogInCallback;
import com.avos.avoscloud.SaveCallback;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.chat.ui.chat.ChatRoomActivity;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.http.RestClient;
import com.dimo.utils.DeviceUtil;
import com.dimo.utils.FileUtil;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import com.squareup.okhttp.Request;
import com.yuan.cp.activity.ClipPictureActivity;
import com.yuan.skeleton.R;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.base.BaseFragmentActivity;
import com.yuan.skeleton.bean.PayInfo;
import com.yuan.skeleton.common.Constants;
import com.yuan.skeleton.event.WebBroadcastEvent;
import com.yuan.skeleton.payment.AliPay;
import com.yuan.skeleton.ui.fragment.WebViewBaseFragment;
import com.yuan.skeleton.ui.fragment.WebViewFragment;
import com.victor.loading.rotate.RotateLoading;
import com.yuan.skeleton.ui.view.PickerPopWindow;
import com.yuan.skeleton.utils.JsonParse;
import com.yuan.skeleton.utils.OkHttpClientManager;
import com.yuan.skeleton.utils.ToastUtil;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;
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
    public static final int kActivityRequestCodeImagePicker = 9;

    private final int SYS_INTENT_REQUEST = 0XFF01;
    private final int CAMERA_INTENT_REQUEST = 0XFF02;
    private final int FILE_INTENT_REQUEST = 0XFF03;

    private String capturePath;
    private Double clipRatio;
    private String clipHeight;
    private String clipWidth;

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
    private AliPay aliPay;
    private String pay_type;

    private Calendar calendar;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (pay_type.equals("alipay")) {
                aliPay.AlipayResultProcess(msg);
            }
            else if (pay_type.equals("wechatpay")) {
//                WechatpayResultProcess(msg);
            }
        };
    };

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

        bridge.registerHandler("purchase", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("reponse.data",data);
                Map<String,Object> params = null;
                Map<String,Object> orderMap = null;
                Map<String,Object> orderPackagesMap = null;
                try {
//                    params = (Map<String, Object>) JsonUtils.newInstance().readJson2List(data);
//                    orderMap = (Map<String, Object>) params.get("order");
//                    List<Object> orderPackagesList = (List<Object>) orderMap.get("order_packages");
//                    orderPackagesMap = (Map<String, Object>) orderPackagesList.get(0);
                }  catch (Exception e) {
                    e.printStackTrace();
                }

                String type = "alipay";
//                String type = (String) params.get("type");
                pay_type = type;
                if (type.equals("alipay")) {
                    PayInfo payInfo = new PayInfo();
//                    payInfo.setOrderNo(orderMap.get("order_no").toString());
//                    payInfo.setProduct_desc(orderPackagesMap.get("package_name").toString()+ orderPackagesMap.get("total_num").toString() + "张");
                    payInfo.setOrderNo("123332222");
                    payInfo.setProduct_desc("测试测试测试");
                    payInfo.setProduct_name("支付Title");
                    payInfo.setTotal_fee("0.01");
//                    payInfo.setTotal_fee(String.valueOf(((Integer) orderMap.get("total_fee") / 100)));
                    aliPay = new AliPay(
                            payInfo,
                            mContext,
                            WebViewBasedActivity.this
                    );
                    aliPay.setHandler(mHandler);
                    aliPay.setPayCallback(jsCallback);
                    aliPay.pay();       //支付
                }

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

        bridge.registerHandler("showToast", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                ToastUtil.showShort(mContext,data);
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

        bridge.registerHandler("selectImages", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("showImagePicker got:" + data);
                mCallback = callback;

                startImagePicker();
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
                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final String text = params.get("type");
//                final String content = params.get("content").toString().substring(0,params.get("content").indexOf("."));
                final String content = params.get("content").toString();
                if (!TextUtils.isEmpty(text) && text.equals("icon")) {
                    Resources resources = getResources();
                    String icon = content.substring(0,params.get("content").indexOf("."));
                    int resourceId = resources.getIdentifier(icon,"drawable",getPackageName());
                    setRightItem(resourceId, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bridge.callHandler("onRightItemClick");
                        }
                    });
                } else {
                    // show a text button
                    setRightItem(content, new View.OnClickListener() {
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

//                try {
//                    JSONObject jo = new JSONObject(data);
//                    JSONObject object = jo.getJSONObject("headers");
//                    if (object == null || object.length() == 0) {
//                        return;
//                    } else {
////                        String token = object.getString("authtoken");
//
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//                JSONObject params = null;
//                try {
//                    params = new JSONObject(data);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                RestClient.getInstance().bridgeRequest(params, RestClient.METHOD_GET, callback);
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

                    String key = params.get("key");
                    String value = params.get("value");

                    if (value == null || value.equals("null"))
                        prefs.edit().remove(key).commit();
                    else
                        prefs.edit().putString(key, value).commit();

                    if (null != callback) {
                        callback.callback(null);
                    }

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

                String objectId = params.get("lean_id");
                String userId = params.get("user_id");
                String houseId = params.get("house_id");
                if(params.get("audit_type") != null)
                    prefs.edit().putString("auditType",params.get("audit_type")).commit();
                prefs.edit().putString("houseId",houseId).commit();
                prefs.edit().putString("target_id",userId).commit();

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
                            obj.put("city",location.getCity());
                            obj.put("district",location.getDistrict());
                            obj.put("province",location.getProvince());
                            obj.put("street",location.getStreet());
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

        bridge.registerHandler("cutImage", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mCallback = jsCallback;

                try {
                    HashMap<String, String> hashMap = StringUtil.JSONString2HashMap(data);
                    clipHeight = hashMap.get("height");
                    clipWidth = hashMap.get("width");
                    clipRatio = Integer.valueOf(clipHeight).doubleValue() / Integer.valueOf(clipWidth).doubleValue();


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //1.弹出选择图片功能
                new AlertDialog.Builder(mContext).setTitle(R.string.dialog_title)
                        .setPositiveButton(R.string.dialog_camera, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //进入相机
                                systemCamera();
                                dialog.dismiss();
                            }
                        }).setNegativeButton(R.string.dialog_photo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //进入相册
                        systemPhoto();
                        dialog.dismiss();
                    }
                }).show();

            }
        });

        bridge.registerHandler("broadcast", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(final String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                EventBus.getDefault().post(new WebBroadcastEvent(data, WebViewBasedActivity.this));
            }
        });

        //TODO 代码已完善，待测试。
        bridge.registerHandler("uploadFiles",new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                mCallback = jsCallback;

                try {
                    JSONArray jsonArray = new JSONArray(data);
                    File[] files = new File[jsonArray.length()];
                    String [] fileKeys = new String[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++){
                        String fileUrl = (String) jsonArray.get(i);
                        files[i] = new File(fileUrl);
                        fileKeys[i] = "file[]";
                    }
                    String token = getUserToken();
                    OkHttpClientManager.postAsyn(Constants.kWebServiceUploadCommon,
                            new UploadResultCallBack(),
                            files,
                            fileKeys,
                            new OkHttpClientManager.Param[]{
                                    new OkHttpClientManager.Param("token", token),
                                    new OkHttpClientManager.Param("Content-Type", "multipart/form-data")});
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        bridge.registerHandler("reviewImages",new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("图片预览",data);
                Intent intent = new Intent(mContext, ImagePagerActivity.class);
                intent.putExtra("json",data);
                startActivity(intent);
            }
        });

        bridge.registerHandler("showDateTimePicker",new WebViewJavascriptBridge.WVJBHandler(){

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("showDateTimePicker",data);
                mCallback = jsCallback;
                try {
                    HashMap<String, String> map = StringUtil.JSONString2HashMap(data);
                    String date = map.get("pick_date");
                    calendar = Calendar.getInstance();
                    if(date.equals("true")){
                        //选择日期
                        DatePickerDialog.newInstance(new DataPickerOnClickListener(), calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show(mFragmentManager, "datePicker");
                    }else{
                        //选择时间
                        TimePickerDialog.newInstance(new TimePickerOnClickListener(), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(mFragmentManager, "timePicker");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        bridge.registerHandler("logout", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                prefs.edit().putBoolean("isLogin", false).commit();
                if (WebViewActivity.instance != null)
                    WebViewActivity.instance.finish();
                if (MainActivity.instance != null)
                    MainActivity.instance.finish();
                Intent intent = new Intent(mContext, SignInActivity.class);
                startActivity(intent);
                finish();
            }
        });

        bridge.registerHandler("showPickerView",new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i("showPickerView",data);

                ArrayList item1 = new ArrayList();
                ArrayList item2 = new ArrayList();
                ArrayList item3 = new ArrayList();
                try {
                    JSONArray jsonArray = new JSONArray(data);
                    for (int i = 0; i < jsonArray.length(); i++){
                        JSONObject jsonObject = (JSONObject) jsonArray.opt(i);
                        JSONArray jsonRows = (JSONArray) jsonObject.get("rows");
                        for (int j = 0; j < jsonRows.length(); j++){
                            if(i == 0)
                                item1.add(jsonRows.opt(j).toString());
                            else if (i ==1)
                                item2.add(jsonRows.opt(j).toString());
                            else
                                item3.add(jsonRows.opt(j).toString());
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                PickerPopWindow pickPopWin = new PickerPopWindow(mContext, item1, item2, item3, new PickerPopWindow.OnPickCompletedListener() {
                    @Override
                    public void onAddressPickCompleted(String item1, String item2, String item3) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(item1);
                        sb.append(item2);
                        sb.append(item3);
                        Log.i("result",sb.toString());
                    }
                });

                pickPopWin.showPopWin(WebViewBasedActivity.this);

            }
        });
    }

    protected String getUserToken() throws JSONException {
        String json = prefs.getString("userLogin", "");
        HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
        return params.get("token");
    }

    private class UploadResultCallBack extends OkHttpClientManager.ResultCallback<String>{

        @Override
        public void onError(Request request, Exception e) {

        }

        @Override
        public void onResponse(String response) {
            Log.i("response",response);
            mCallback.callback(response);
        }
    }

    protected void startImagePicker() {
        Intent intent = new Intent(mContext, MultiImageSelectorActivity.class);

        // whether show camera
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SHOW_CAMERA, true);

        // max select image amount
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_COUNT, 9);

        // select mode (MultiImageSelectorActivity.MODE_SINGLE OR MultiImageSelectorActivity.MODE_MULTI)
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_MODE, MultiImageSelectorActivity.MODE_MULTI);

        startActivityForResult(intent, kActivityRequestCodeImagePicker);
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

    /*@Override
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
        } else if (requestCode == kActivityRequestCodeImagePicker) {
            // 使用微信风格的集成拍照/图库的图片选择器
            if (resultCode == RESULT_OK) {
                // Get the result list of select image paths
                List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                StringBuilder csvList = new StringBuilder();
                for (String s : path) {
                    csvList.append(s);
                    csvList.append(",");
                }
                prefs.edit().putString("cached_selected_images", csvList.toString()).commit();

                // 选择图片之后保留在本地页面, 并通知页面
                bridge.callHandler("activetyFinished");
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
    }*/

    //FIXME: use updated photo selection widget
    public void startActivityGallery() {

    }

    public void startActivityCamera() {

    }

    //TODO: 接收Web端触发的Event事件
    public void onEvent(WebBroadcastEvent event){
        Toast.makeText(mContext,event.result,Toast.LENGTH_SHORT).show();
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

    /**
     * 打开系统相册
     */
    private void systemPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SYS_INTENT_REQUEST);

    }

    protected void systemCamera(){
        String sdStatus = Environment.getExternalStorageState();
		/* 检测sd是否可用 */
        if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "SD卡不可用！", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String name = formatter.format(System.currentTimeMillis()) + ".jpg";
        capturePath = FileUtil.getPhotoPath() + name;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(capturePath)));
        startActivityForResult(intent, CAMERA_INTENT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = new Intent(mContext, ClipPictureActivity.class);
        intent.putExtra("clipRatio",clipRatio);
        intent.putExtra("clipHeight",clipHeight);
        intent.putExtra("clipWidth",clipWidth);
        if(requestCode == CAMERA_INTENT_REQUEST && resultCode == RESULT_OK){
            intent.putExtra("type","camera");
            intent.putExtra("imageFilePath",capturePath);
            startActivityForResult(intent,FILE_INTENT_REQUEST);
        } else if (requestCode == SYS_INTENT_REQUEST && resultCode == RESULT_OK && data != null){
            Uri uri = data.getData();
            intent.putExtra("type","photo");
            intent.putExtra("imageFilePath",uri.toString());
            startActivityForResult(intent,FILE_INTENT_REQUEST);
        } else if (requestCode == FILE_INTENT_REQUEST
                && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra("filePath");
            mCallback.callback(filePath);
            intent = null;
        } else if (requestCode == kActivityRequestCodeImagePicker) {
            // 使用微信风格的集成拍照/图库的图片选择器
            if (resultCode == RESULT_OK) {
                // Get the result list of select image paths
                List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                StringBuilder csvList = new StringBuilder();
                for (String s : path) {
                    csvList.append(s);
                    csvList.append(",");
                }
                Gson gson = new Gson();

//                prefs.edit().putString("cached_selected_images", csvList.toString()).commit();
                mCallback.callback(gson.toJson(path));
                // 选择图片之后保留在本地页面, 并通知页面
//                bridge.callHandler("activetyFinished");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    protected boolean isUserType() {
        try {
            return JsonParse.getInstance().judgeUserType();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    private class DataPickerOnClickListener implements DatePickerDialog.OnDateSetListener{

        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
            StringBuffer sb = new StringBuffer();
            sb.append(year);
            sb.append("-");
            sb.append(month);
            sb.append("-");
            sb.append(day);
            mCallback.callback(sb.toString());
        }
    }

    private class TimePickerOnClickListener implements TimePickerDialog.OnTimeSetListener{

        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {

        }
    }

}
