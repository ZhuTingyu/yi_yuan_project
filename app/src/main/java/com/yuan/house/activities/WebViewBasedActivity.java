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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.MessageAgent;
import com.avoscloud.leanchatlib.model.AVIMNoticeWithHouseIdMessage;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bugtags.library.Bugtags;
import com.dimo.http.RestClient;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.etiennelawlor.imagegallery.library.ImageGalleryFragment;
import com.etiennelawlor.imagegallery.library.activities.FullScreenImageGalleryActivity;
import com.etiennelawlor.imagegallery.library.activities.ImageGalleryActivity;
import com.etiennelawlor.imagegallery.library.adapters.FullScreenImageGalleryAdapter;
import com.etiennelawlor.imagegallery.library.adapters.ImageGalleryAdapter;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.victor.loading.rotate.RotateLoading;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.base.BaseFragmentActivity;
import com.yuan.house.bean.PayInfo;
import com.yuan.house.common.Constants;
import com.yuan.house.event.PageEvent;
import com.yuan.house.event.WebBroadcastEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.http.WebService;
import com.yuan.house.payment.AliPay;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.FileUtil;
import com.yuan.house.utils.ImageUtil;
import com.yuan.house.utils.ToastUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import me.nereo.multi_image_selector.MultiImageSelector;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/12/15.
 */
public abstract class WebViewBasedActivity extends BaseFragmentActivity implements WebViewFragment.OnFragmentInteractionListener,
        WebViewFragment.OnBridgeInteractionListener, ProposalFragment.OnProposalInteractionListener,
        ImageGalleryAdapter.ImageThumbnailLoader, FullScreenImageGalleryAdapter.FullScreenImageLoader {
    private final int kActivityRequestCodeWebActivity = 3;
    private final int kActivityRequestCodeImagePickOnly = 10;
    private final int kActivityRequestCodeImagePickThenUpload = 11;
    private final int kActivityRequestCodeImagePickThenCropRectangle = 12;
    private final int kActivityRequestCodeImagePickThenCropSquare = 13;
    private final int kActivityRequestCodeImageCrop = 14;
    private final int kActivityRequestCodeSelectMapLocation = 20;
    protected FragmentManager mFragmentManager;
    protected FragmentTransaction mFragmentTransaction;
    @BindView(R.id.rotateloading)
    protected RotateLoading mLoadingDialog;
    String mUrl;
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
    private WebViewJavascriptBridge.WVJBResponseCallback mBridgeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.inject(this);

        mContext = this;

        // prepare the fragment for main framelayout
        mFragmentManager = getSupportFragmentManager();

        JSONObject params = null;
        String url = Constants.kWebPageEntry;

        Bundle bundle = getIntent().getExtras();

        if (!TextUtils.isEmpty(url)) {
            setContentView(R.layout.activity_webview, true);
        } else {
            setContentView(R.layout.activity_webview);
        }

        if (bundle != null && !TextUtils.isEmpty(bundle.getString("params"))) {
            try {
                params = new JSONObject(bundle.getString("params"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (params != null) {
                try {
                    mUrl = bundle.getString("url");

                    JSONObject object = new JSONObject(params.optString("params"));

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

        ButterKnife.bind(this);

        if (mFragmentTransaction == null) {
            mFragmentTransaction = mFragmentManager.beginTransaction();
            mFragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        // configure image gallery showing
        ImageGalleryActivity.setImageThumbnailLoader(this);
        ImageGalleryFragment.setImageThumbnailLoader(this);
        FullScreenImageGalleryActivity.setFullScreenImageLoader(this);
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

    protected void hideSoftInputView() {
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                manager.hideSoftInputFromWindow(currentFocus.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    protected void setSoftInputMode() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * Invoke from JS script interaction
     *
     * @param url    destination url
     * @param params params for page
     */
    public void openLinkInNewActivity(String url, JSONObject params) {
        Bundle extras = new Bundle();
        extras.putString("params", params.toString());

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("params", params.toString());
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

    //TODO: 接收 Web 端触发的 Event 事件
    public void onEvent(WebBroadcastEvent event) {
        Toast.makeText(mContext, event.result, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == kActivityRequestCodeWebActivity) {
            Timber.v("kActivityRequestCodeWebActivity");

            String result = null;
            if (data != null) {
                // handle the case if activity is terminated by JS code
                Bundle res = data.getExtras();
                result = res.getString("param_result_after_activity_finished");
            }

            Timber.v("Got finished result:" + result);

            // send back the result to original webview
            getWebViewFragment().getBridge().callHandler("activityFinished", result);
            return;
        } else if (requestCode == kActivityRequestCodeImagePickOnly) {
            Timber.v("kActivityRequestCodeImagePickOnly");
            if (resultCode == RESULT_OK) {
                // 获取到选取照片的本地文件路径
                List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                // /storage/emulated/0/DCIM/IMG_-646584368.jpg

                mBridgeCallback.callback(path);
            }
        } else if (requestCode == kActivityRequestCodeImagePickThenUpload) {
            // TODO: 16/6/9 upload files directly
            Timber.v("kActivityRequestCodeImagePickThenUpload");
            List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            // TODO: 16/6/10 invoke upload process
            uploadMultiPartFiles(path);
        } else if (requestCode == kActivityRequestCodeImagePickThenCropRectangle
                || requestCode == kActivityRequestCodeImagePickThenCropSquare) {
            // TODO: 16/6/9 upload files directly
            Timber.v("kActivityRequestCodeImagePickThenUpload");
            List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            if (path == null) return;

            Intent intent = new Intent(mContext, CropActivity.class);
            intent.putExtra(Constants.kBundleExtraCropImageType, requestCode);
            intent.putExtra(Constants.kBundleExtraCropImageName, path.get(0));
            startActivityForResult(intent, kActivityRequestCodeImageCrop);
        } else if (requestCode == kActivityRequestCodeImageCrop) {
            // handle cropped image
            String path = data.getStringExtra("data");
            JSONArray datum = new JSONArray();
            datum.put(path);
            mBridgeCallback.callback(datum.toString());
        } else if (requestCode == kActivityRequestCodeSelectMapLocation) {
            Timber.v("kActivityRequestCodeSelectMapLocation");
            // reverse callback the selected map location
            String result;
            if (data != null) {
                // handle the case if activity is terminated by JS code
                Bundle res = data.getExtras();
                result = res.getString(Constants.kActivityParamFinishSelectLocationOnMap);
                try {
                    JSONObject object = new JSONObject(result);
                    getWebViewFragment().getBridge().callHandler("selectedMapLocation", object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // never reach
            Timber.e("onActivityResult SHOULD NEVER REACH");
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    public void onBridgeSelectImageFromNative(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
        mBridgeCallback = callback;

        int requestCode = kActivityRequestCodeImagePickOnly;
        MultiImageSelector selector = MultiImageSelector.create(mContext)
                .showCamera(true) // show camera or not. true by default
                .count(9);// max select image size, 9 by default. used width #.multi()
        try {
            JSONObject object = new JSONObject(data);
            String type = object.optString("type");
            if (Constants.kImageCropTypeRectangle.equals(type)) {
                selector = selector.single(); // single mode
                requestCode = kActivityRequestCodeImagePickThenCropRectangle;
            } else if (Constants.kImageCropTypeSquare.equals(type)) {
                selector = selector.single(); // single mode
                requestCode = kActivityRequestCodeImagePickThenCropSquare;
            } else if (Constants.kImageCropTypeNone.equals(type)) {
                selector = selector.multi(); // single mode
            }

            selector.start(this, requestCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onBridgeOpenNewLink(String url, JSONObject params) {
        openLinkInNewActivity(url, params);
    }

    public void onBridgeShowSearchBar() {
        EditText searchBar = setTitleSearch();
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String content = v.getText().toString();
                    if (!TextUtils.isEmpty(content)) {
                        getWebViewFragment().getBridge().callHandler("searchContent", content);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void onBridgeSendNoticeMessage(final String data) {
        boolean isAgency = false;
        // TODO: 16/6/10 web传参数（house_id,lean_id(数组),text(消息文本)）,用于从web端发送消息给特定的用户
        List<String> leanIdList = null;
        String houseId = null;
        try {
            JSONObject object = new JSONObject(data);
            houseId = object.optString("house_id");
            String leanId = object.optString("lean_id");
            String text = object.optString("text");

            leanIdList = new ArrayList<>(Arrays.asList(leanId.split(",")));
            if ("agency".equals(object.optString("type"))) {
                isAgency = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // TODO: 16/6/10 get last user id??? WTH?
        String leanIdString = leanIdList.get(leanIdList.size() - 1);

        // FIXME: 16/6/10 WTF!!! isAgency 的作用是什么? 中介不能发这种类型消息么?
        // 创建相应对话, 并发送文本信息到该会话
        final String finalHouseId = houseId;
        ChatManager.getInstance().fetchConversationWithUserId(null, leanIdString, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation avimConversation, AVIMException e) {
                AVIMNoticeWithHouseIdMessage message = new AVIMNoticeWithHouseIdMessage();
                message.setHouseId(finalHouseId);

                MessageAgent messageAgent = new MessageAgent(avimConversation);
                messageAgent.sendEncapsulatedTypedMessage(message);

                // TODO: 16/6/10 发送成功之后需要缓存该条消息到本地
            }
        });
    }

    public void onBridgeLogout() {
        DMApplication.getInstance().logout();
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

    public void onBridgeUploadFiles(List<String> datum) {
        uploadMultiPartFiles(datum);
    }

    public void onBridgeResizeOrCropImage() {

    }

    public void onBridgeDismissProgressDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isStart())
            mLoadingDialog.stop();
    }

    public void onBridgeFinishActivity(String data) {
        HashMap<String, Object> params = null;
        try {
            params = StringUtil.JSONString2HashMap(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String result = (String) params.get("result");

        Bundle conData = new Bundle();
        conData.putString("param_result_after_activity_finished", result);

        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    public void onBridgeShowImageGallery(List<String> images) {
        Intent intent = new Intent(this, ImageGalleryActivity.class);

        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ImageGalleryActivity.KEY_IMAGES, new ArrayList<>(images));
        bundle.putString(ImageGalleryActivity.KEY_TITLE, "图片库");
        intent.putExtras(bundle);

        startActivity(intent);
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
        HashMap<String, Object> params = null;
        try {
            params = StringUtil.JSONString2HashMap(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String link = (String) params.get("link");
        if (!TextUtils.isEmpty(link)) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(browserIntent);
        }
    }

    public void onBridgeSelectMapLocation() {
        Intent intent = new Intent(mContext, MapActivity.class);
        startActivityForResult(intent, kActivityRequestCodeSelectMapLocation);
    }

    public void onBridgeUpdateFriendRelationship() {
        EventBus.getDefault().post(new PageEvent(PageEvent.PageEventEnum.FRIENDSHIP_UPDATE, null));
    }

    public void onBridgeDropToMessage() {
        EventBus.getDefault().post(new PageEvent(PageEvent.PageEventEnum.DROP_TO_MESSAGE, null));
    }

    @Override
    public void onBridgeSignIn(String data) {
        throw new NotImplementedException("NOT IMPLEMENTED");
    }

    private RequestParams constructMultiPartParams(List<String> filePaths) {
        // TODO: 16/6/14 failed to upload files
        RequestParams params = new RequestParams();

        // loopj android async http support add byte[] as RequestParam item to submit multipart data
        for (String file : filePaths) {
            byte[] data = new byte[0];

            String fileType = FileUtil.getFileExt(file);

            if (!fileType.equals("amr")) {
                // image
                data = ImageUtil.compressToByteArray(file);
            } else {
                // audio
                try {
                    data = FileUtils.readFileToByteArray(new File(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                params.put("file[]", new File(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return params;
    }

    @Override
    public void onUploadProposalAudio(String data) {
//        if (msg_type == ProposalMediaType.AUDIO)
//            this.duration = MediaPlayer.create(this, Uri.parse(data)).getDuration();
        List<String> datum = new ArrayList<>();
        datum.add(data);
        uploadMultiPartFiles(datum);
    }

    /**
     * 上传文件列表
     *
     * @param filenames
     */
    private void uploadMultiPartFiles(List<String> filenames) {
        RequestParams entity = constructMultiPartParams(filenames);

        WebService.getInstance().postMultiPartFormDataFile(entity, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);

                ToastUtil.showShort(mContext, "提交成功");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);

                ToastUtil.showShort(mContext, "提交失败");
            }
        });
    }

    @Override
    public void onSelectImageForProposal() {
        int requestCode = kActivityRequestCodeImagePickThenUpload;
        MultiImageSelector.create(mContext)
                .showCamera(true) // show camera or not. true by default
                .count(9) // max select image size, 9 by default. used width #.multi()
                .multi()
                .start(this, requestCode);
    }

    protected boolean filterException(Exception e) {
        if (e != null) {
            ToastUtil.show(mContext, e.getMessage());
            return false;
        } else {
            return true;
        }
    }

    protected void restGet(String url, AsyncHttpResponseHandler responseHandler) {
        RestClient.getInstance().get(url, AuthHelper.authTokenJsonHeader(), responseHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bugtags.onResume(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bugtags.onPause(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Bugtags.onDispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    // region ImageGalleryAdapter.ImageThumbnailLoader Methods
    public void loadImageThumbnail(ImageView iv, String imageUrl, int dimension) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.with(iv.getContext())
                    .load(imageUrl)
//                    .resize(dimension, dimension)
//                    .centerCrop()
                    .into(iv);
        } else {
            iv.setImageDrawable(null);
        }
    }

    // region FullScreenImageGalleryAdapter.FullScreenImageLoader
    public void loadFullScreenImage(final ImageView iv, String imageUrl, int width, final LinearLayout bgLinearLayout) {
        if (!TextUtils.isEmpty(imageUrl)) {
            Picasso.with(iv.getContext())
                    .load(imageUrl)
//                    .resize(width, 0)
                    .into(iv, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {

                        }
                    });
        } else {
            iv.setImageDrawable(null);
        }
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
    // endregion

    private class TimePickerOnClickListener implements TimePickerDialog.OnTimeSetListener {
        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {

        }
    }
    // endregion


}
