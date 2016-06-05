package com.yuan.house.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import com.victor.loading.rotate.RotateLoading;
import com.yuan.house.application.Injector;
import com.yuan.house.base.BaseFragmentActivity;
import com.yuan.house.bean.PayInfo;
import com.yuan.house.common.Constants;
import com.yuan.house.event.WebBroadcastEvent;
import com.yuan.house.payment.AliPay;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.JsonParse;
import com.yuan.skeleton.R;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/12/15.
 */
public abstract class WebViewBasedActivity extends BaseFragmentActivity implements WebViewFragment.OnFragmentInteractionListener, WebViewFragment.OnBridgeInteractionListener {
    public static final int kActivityRequestCodeWebActivity = 3;

    protected FragmentManager mFragmentManager;
    protected FragmentTransaction mFragmentTransaction;
    protected String mUrl;

    @InjectView(R.id.rotateloading)
    protected RotateLoading mLoadingDialog;

    WebViewBaseFragment webViewFragment;

    private AliPay aliPay;
    private String pay_type;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (pay_type.equals("alipay")) {
                aliPay.AlipayResultProcess(msg);
            } else if (pay_type.equals("wechatpay")) {
//                WechatpayResultProcess(msg);
            }
        }
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

    protected String getUserToken() throws JSONException {
        String json = prefs.getString("userLogin", "");
        HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
        return params.get("token");
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

    public WebViewBaseFragment getWebViewFragment() {
        return webViewFragment;
    }

    @Override
    public void onFragmentInteraction(final WebViewBaseFragment fragment) {
        Timber.v("Fragment ready to use");

        webViewFragment = fragment;
    }

    //TODO: 接收Web端触发的Event事件
    public void onEvent(WebBroadcastEvent event) {
        Toast.makeText(mContext, event.result, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    public void onBridgeRequestPurchase(WebViewJavascriptBridge.WVJBResponseCallback callback) {
//        Map<String, Object> params = null;
//        Map<String, Object> orderMap = null;
//        Map<String, Object> orderPackagesMap = null;
//        try {
//            params = (Map<String, Object>) JsonUtils.newInstance().readJson2List(data);
//            orderMap = (Map<String, Object>) params.get("order");
//            List<Object> orderPackagesList = (List<Object>) orderMap.get("order_packages");
//            orderPackagesMap = (Map<String, Object>) orderPackagesList.get(0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

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
            aliPay.setPayCallback(callback);

            aliPay.pay();
        }
    }

    public void onBridgeSelectImageFromNative() {

    }

    public void onBridgeOpenNewLink(String url, HashMap<String, String> params) {
        openLinkInNewActivity(url, params);
    }

    public void onBridgeShowSearchBar() {

    }

    public void onBridgeLogout() {

    }

    public void onBridgeShowProgressDialog() {
        mLoadingDialog.start();
    }

    public void onBridgeSetTitle(String title) {
        setTitleItem(title);
    }

    public void onBridgeSetRightItem(int resourceId, View.OnClickListener onRightItemClick) {
        setRightItem(resourceId, onRightItemClick);
    }

    public void onBridgeSetRightItem(String text, View.OnClickListener onRightItemClick) {
        setRightItem(text, onRightItemClick);
    }

    public void onBridgeUploadFiles() {

    }

    public void onBridgeResizeOrCropImage() {

    }

    public void onBridgeDismissProgressDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isStart())
            mLoadingDialog.stop();
    }

    public void onBridgeFinishActivity(String data) {
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

    public void onBridgeRequestLocation(final WebViewJavascriptBridge.WVJBResponseCallback callback) {
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
                    obj.put("city", location.getCity());
                    obj.put("district", location.getDistrict());
                    obj.put("province", location.getProvince());
                    obj.put("street", location.getStreet());
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

    public void onBridgeOpenNewLinkWithExternalBrowser(String data) {
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
    }

    public void onBridgeSelectMapLocation() {
//        Intent intent = new Intent(mContext, MapActivity.class);
//        startActivityForResult(intent, REQUEST_MAP_CODE);
    }

    @Override
    public void onBridgeUpdateFriendRelationship() {
        throw new NotImplementedException("NOT IMPLEMENTED");
    }

    @Override
    public void onBridgeDropToMessage() {
        throw new NotImplementedException("NOT IMPLEMENTED");
    }

    @Override
    public void onBridgeSignIn(String data) {
        throw new NotImplementedException("NOT IMPLEMENTED");
    }

    private class DataPickerOnClickListener implements DatePickerDialog.OnDateSetListener {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
            StringBuffer sb = new StringBuffer();
            sb.append(year);
            sb.append("-");
            sb.append(month);
            sb.append("-");
            sb.append(day);
//            mCallback.callback(sb.toString());
        }
    }

    private class TimePickerOnClickListener implements TimePickerDialog.OnTimeSetListener {
        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {

        }
    }
}
