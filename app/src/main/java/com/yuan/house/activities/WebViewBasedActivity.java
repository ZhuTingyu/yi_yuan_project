package com.yuan.house.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMConversationQuery;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationQueryCallback;
import com.avos.avoscloud.im.v2.callback.AVIMSingleMessageQueryCallback;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.MessageAgent;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.model.AVIMHouseMessage;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baoyz.actionsheet.ActionSheet;
import com.bugtags.library.Bugtags;
import com.dimo.utils.DateUtil;
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
import com.tasomaniac.android.widget.DelayedProgressDialog;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.base.BaseFragmentActivity;
import com.yuan.house.bean.PayInfo;
import com.yuan.house.common.Constants;
import com.yuan.house.event.NotificationEvent;
import com.yuan.house.event.PageEvent;
import com.yuan.house.event.WebBroadcastEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.http.RestClient;
import com.yuan.house.http.WebService;
import com.yuan.house.payment.AliPay;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.FileUtil;
import com.yuan.house.utils.ImageUtil;
import com.yuan.house.utils.SystemServiceUtil;
import com.yuan.house.utils.ToastUtil;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private final int kActivityRequestCodeImageCrop = 14;
    private final int kActivityRequestCodeSelectMapLocation = 20;
    protected FragmentManager mFragmentManager;
    protected FragmentTransaction mFragmentTransaction;
    protected DelayedProgressDialog mLoadingDialog;
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

                    if (!TextUtils.isEmpty(object.optString("hasBackButton"))) {
                        if (object.optString("hasBackButton").equals("true")) {
                            setLeftItem(R.drawable.btn_back, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Timber.v("OnClick back button");
                                    finish();
                                }
                            });
                        }
                    }

                    if (!TextUtils.isEmpty(object.optString("title"))) {
                        setTitleItem(object.optString("title"));
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

        KeyboardVisibilityEvent.setEventListener(this, new KeyboardVisibilityEventListener() {
            @Override
            public void onVisibilityChanged(boolean isOpen) {
                WebView webView = getWebViewFragment().getWebView();

                if (!isOpen) {
                    webView.clearFocus();
                }
            }
        });
    }

    protected void switchToFragment(String tag) {
        // use other method to keep the old fragment than use this simple and rude `replace`
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.replace(R.id.content_frame, getFragment(tag), tag);
        mFragmentTransaction.commit();
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

    protected void hideSoftInputView() {
        SystemServiceUtil.hideSoftInputView(this);
    }

    protected void setSoftInputMode() {
        SystemServiceUtil.setSoftInputMode(this);
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

        Class cls = WebViewActivity.class;

        if (url.indexOf("agency_check_contract.html") == 0) {
            cls = SegmentalWebActivity.class;
        }
        Intent intent = new Intent(this, cls);
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

    // 接收 Web 端触发的 Event 事件
    public void onEvent(WebBroadcastEvent event) {
        Timber.v(event.result);
    }

    public void onEvent(PageEvent event) {
        if (event.getEventType() == PageEvent.PageEventEnum.REDIRECT) {
            JSONObject object = new JSONObject();
            try {
                object.put("hasBackButton", true);
                object.put("title", "");

                JSONObject params = new JSONObject();
                params.put("params", object);

                openLinkInNewActivity(event.getHolder(), params);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onEvent(NotificationEvent event) {
        if (event.getEventType() == NotificationEvent.NotificationEventEnum.HOUSE_RECOMMENDED_MESSAGE) {
            getWebViewFragment().getBridge().callHandler("RecommendedNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.NEW_HOUSE_AUDIT) {
            getWebViewFragment().getBridge().callHandler("AuditorNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.NEW_EXCLUSIVE_CONTRACT) {
            getWebViewFragment().getBridge().callHandler("AuditorNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.NEW_PREORDER_CONTRACT) {
            JSONObject object = new JSONObject();
            try {
                object.put("holder", event.getHolder());
                object.put("auditType", 3);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            getWebViewFragment().getBridge().callHandler("AuditorNotification", object);
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.NEW_BUSINESS_CONTRACT) {
            JSONObject object = new JSONObject();
            try {
                object.put("holder", event.getHolder());
                object.put("auditType", 4);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            getWebViewFragment().getBridge().callHandler("AuditorNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.KICK_OUT) {
            JSONObject object = event.getHolder();

            if (object == null || (AuthHelper.getInstance().userAlreadyLogin() && !AuthHelper.getInstance().getUserToken().equals(object.optString("exclusive_token")))) {
                Toast.makeText(mContext, "您的账号在别处登陆", Toast.LENGTH_SHORT).show();

                DMApplication.getInstance().kickOut();
            }
        }
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

                JSONArray objects = new JSONArray();
                for (int i = 0; i < path.size(); i++) {
                    objects.put(path.get(i));
                }
                mBridgeCallback.callback(objects.toString());
            }
        } else if (requestCode == kActivityRequestCodeImagePickThenUpload) {
            // TODO: 16/6/9 upload files directly
            Timber.v("kActivityRequestCodeImagePickThenUpload");
            List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            // TODO: 16/6/10 invoke upload process
            //nativeUploadMultiPartFiles(path);

            Fragment fragment = getFragment(Constants.kFragmentTagProposal);
            if (fragment != null) {
                ProposalFragment pfragment = (ProposalFragment) fragment;
                String fileName = path.get(0);
                pfragment.uploadFile(fileName);
            }

        } else if (requestCode == Constants.kActivityRequestCodeImagePickThenCropRectangle
                || requestCode == Constants.kActivityRequestCodeImagePickThenCropSquare) {
            Timber.v("kActivityRequestCodeImagePickThenUpload");
            if (data == null) return;

            List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            if (path == null) return;

            Intent intent = new Intent(mContext, CropActivity.class);
            intent.putExtra(Constants.kBundleExtraCropImageType, requestCode);
            intent.putExtra(Constants.kBundleExtraCropImageName, path.get(0));
            startActivityForResult(intent, kActivityRequestCodeImageCrop);
        } else if (requestCode == kActivityRequestCodeImageCrop) {
            // handle cropped image
            if (data != null) {
                String path = data.getStringExtra("data");
                JSONArray datum = new JSONArray();
                datum.put(path);
                mBridgeCallback.callback(datum.toString());
            }
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

    public void onBridgeRequestPurchase(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
        JSONObject object;
        try {
            object = new JSONObject(data);

            String type = object.optString("type");

            pay_type = type;
            if (type.equals("alipay")) {
                JSONObject order = object.optJSONObject("order");
                PayInfo payInfo = new PayInfo();

                payInfo.setOrderNo(order.optString("order_no"));
                payInfo.setProduct_name(order.optString("title"));
                payInfo.setProduct_desc(order.optString("content"));
// FIXME: 测试环境统一为 1 分钱
//                payInfo.setTotal_fee(order.optString("total_fee"));
                payInfo.setTotal_fee("0.01");

                aliPay = new AliPay(payInfo, mContext, WebViewBasedActivity.this);
                aliPay.setHandler(mHandler);
                aliPay.setPayCallback(callback);

                aliPay.pay();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBridgeShowErrorMessage(JSONObject data) {
        // TODO: 16/7/17 加上错误样式
        ToastUtil.showShort(this, data.optString("msg"));
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
                requestCode = Constants.kActivityRequestCodeImagePickThenCropRectangle;
            } else if (Constants.kImageCropTypeSquare.equals(type)) {
                selector = selector.single(); // single mode
                requestCode = Constants.kActivityRequestCodeImagePickThenCropSquare;
            } else if (Constants.kImageCropTypeNone.equals(type)) {
                selector = selector.multi(); // multi image mode
            }

            selector.start(this, requestCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onBridgeOpenNewLink(String url, JSONObject params) {
        openLinkInNewActivity(url, params);
    }

    @Override
    public void onBridgeReplaceLink(String url, JSONObject object) {
        if (getWebViewFragment() != null) {
            getWebViewFragment().redirectToLoadUrl(url);
        }
    }

    public void onBridgeShowSearchBar(String data) {
        setTitleSearch();
    }

    @Override
    public void onBridgeShowActionSheet(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        ArrayList<String> list = new ArrayList<>();

        JSONArray datum;
        try {
            datum = new JSONArray(data);
            for (int i = 0; i < datum.length(); i++) {
                list.add(datum.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ActionSheet.createBuilder(mContext, getSupportFragmentManager())
                .setCancelButtonTitle(R.string.cancel)
                .setOtherButtonTitles(list.toArray(new String[list.size()]))
                .setCancelableOnTouchOutside(true)
                .setListener(new ActionSheet.ActionSheetListener() {
                    @Override
                    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {
                        actionSheet.dismiss();
                    }

                    @Override
                    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
                        jsCallback.callback(index);

                        actionSheet.dismiss();
                    }
                }).show();
    }

    @Override
    public void onBridgeGetRecentChatList(String data, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        AVIMConversationQuery query = ChatManager.getInstance().getImClient().getQuery();
        // FIXME: 16/7/9 只获取最近的 20 条对话
        query.limit(20);
        query.findInBackground(new AVIMConversationQueryCallback() {
            @Override
            public void done(List<AVIMConversation> convs, AVIMException e) {
                if (e == null) {
                    // 处理并返回相应格式的会话列表
                    final JSONArray array = new JSONArray();

                    for (int i = 0; i < convs.size(); i++) {
                        final AVIMConversation conversation = convs.get(i);
                        conversation.getLastMessage(new AVIMSingleMessageQueryCallback() {
                            @Override
                            public void done(AVIMMessage avimMessage, AVIMException e) {
                                if (avimMessage == null) return;

                                AVIMTypedMessage message = (AVIMTypedMessage) avimMessage;

                                JSONObject object = new JSONObject();

                                String otherId = "0";
                                com.alibaba.fastjson.JSONArray ids = (com.alibaba.fastjson.JSONArray) conversation.getAttribute("userIds");
                                if (ids == null || ids.size() == 0) {
                                    otherId = "0";
                                } else {
                                    for (int j = 0; j < ids.size(); j++) {
                                        if (Integer.parseInt(AuthHelper.getInstance().getUserId()) != ids.getInteger(j)) {
                                            otherId = ids.getString(j);
                                            break;
                                        }
                                    }
                                }

                                try {
                                    object.put("date", DateUtil.getDate(message.getTimestamp()));
                                    object.put("message", MessageHelper.outlineOfMsg(message));
                                    object.put("is_read", true);
                                    object.put("otherId", conversation.getMembers().get(0));
                                    object.put("auditType", "0");
                                    object.put("houseId", conversation.getAttribute("houseId"));
                                    object.put("otherUserId", otherId);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }

                                array.put(object);
                            }
                        });
                    }

                    jsCallback.callback(array.toString());
                }
            }
        });
    }

    private void sendHouseInfoMessage(String peerId, final JSONObject info) {
        ChatManager.getInstance().fetchConversationWithUserId(info, peerId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation avimConversation, AVIMException e) {
                AVIMHouseMessage message = new AVIMHouseMessage();

                Map<String, Object> attrs = new HashMap<>();
                attrs.put("houseId", info.optString("house_id"));
                JSONArray images = info.optJSONArray("images");

                if (images == null || images.length() == 0) {
                    attrs.put("houseImage", null);
                } else {
                    attrs.put("houseImage", images.optString(0));
                }

                attrs.put("houseName", info.optString("title"));
                attrs.put("houseAddress", info.optString("location_text"));
                attrs.put("recommended", true);
                attrs.put("recommendedId", info.optString("re_id"));

                message.setAttrs(attrs);

                MessageAgent messageAgent = new MessageAgent(avimConversation);
                messageAgent.sendEncapsulatedTypedMessage(message);

                // 发送成功之后需要缓存该条消息到本地
                ChatManager.getInstance().storeLastMessage(message);
            }
        });
    }

    @Override
    public void onBridgeSendRecommendedMessage(String data) {
        JSONArray userArray = null;
        JSONObject houseInfo = null;

        JSONObject object;
        try {
            object = new JSONObject(data);
            userArray = object.optJSONArray("data");
            houseInfo = object.optJSONObject("house_info");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assert userArray != null;

        // send message to each receipt
        for (int i = 0; userArray.length() > i; i++) {
            sendHouseInfoMessage(userArray.optString(i), houseInfo);
        }
    }

    @Override
    public void onBridgeSendNoticeMessage(final String data, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
        boolean isAgency = false;

        String houseId = null;
        JSONArray leanIdList = null;
        String text = null;
        JSONObject rawObject = null;
        JSONArray userIdList = null;

        try {
            rawObject = new JSONObject(data);
            houseId = rawObject.optString("house_id");
            leanIdList = rawObject.optJSONArray("lean_id");
            userIdList = rawObject.optJSONArray("user_id");
            text = rawObject.optString("text");

            if ("agency".equals(rawObject.optString("type"))) {
                isAgency = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 创建相应对话, 并发送文本信息到该会话
        final String finalHouseId = houseId;
        final String finalText = text;

        for (int i = 0; i < leanIdList.length(); i++) {
            String leanIdString = leanIdList.optString(i);

            JSONObject object = new JSONObject();
            try {
                object.put("lean_id", leanIdString);
                object.put("user_id", userIdList.optString(i));
                object.put("house_id", houseId);
                object.put("text", text);
                object.put("type", rawObject.optString("type"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final int finalI = i;
            final JSONArray finalLeanIdList = leanIdList;
            ChatManager.getInstance().fetchConversationWithUserId(object, leanIdString, new AVIMConversationCreatedCallback() {
                @Override
                public void done(AVIMConversation avimConversation, AVIMException e) {
                    AVIMTextMessage message = new AVIMTextMessage();

                    Map<String, Object> attrs = new HashMap<>();
                    attrs.put("houseId", finalHouseId);
                    attrs.put("username", "wo");

                    message.setAttrs(attrs);

                    message.setText(finalText);

                    MessageAgent messageAgent = new MessageAgent(avimConversation);
                    messageAgent.sendEncapsulatedTypedMessage(message);

                    // 发送成功之后需要缓存该条消息到本地
                    ChatManager.getInstance().storeLastMessage(message);

                    if (finalI == (finalLeanIdList.length() - 1)) {
                        callback.callback("onBridgeSendNoticeMessage finished");
                    }
                }
            });
        }
    }

    public void onBridgeLogout() {
        DMApplication.getInstance().logout();
    }

    public void onBridgeShowProgressDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = DelayedProgressDialog.showDelayed(mContext, null, getString(R.string.loading), true, true);
        } else {
            mLoadingDialog.show();
        }
    }

    public void onBridgeDismissProgressDialog() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
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

    public void onBridgeUploadFiles(String datum, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        // TODO: 16/7/14 parse datum

        nativeUploadImageFiles(datum, jsCallback);
    }

    public void onBridgeResizeOrCropImage() {

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
        Intent intent = new Intent(this, ImageViewPagerActivity.class);

        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ImageViewPagerActivity.KEY_IMAGES, new ArrayList<>(images));
        bundle.putString(ImageViewPagerActivity.KEY_TITLE, "图片库");
        intent.putExtras(bundle);

        startActivity(intent);
    }

    public void onBridgeRequestLocation(final WebViewJavascriptBridge.WVJBResponseCallback callback) {
        final LocationClient locationClient = new LocationClient(getApplicationContext());
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

    public void onBridgeSelectMapLocation(String data) {
        Intent intent = new Intent(mContext, MapActivity.class);

        if (!TextUtils.isEmpty(data)) {
            intent.putExtra("location", data);
        }

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

    @Override
    public void onBridgeUpdateUserMessage(String data) {
        SharedPreferences.Editor editor = prefs.edit();
        try {
            JSONObject object = new JSONObject(data);
            String key = object.optString("key");
            String value = object.getString("value");
            editor.putString(key, value);
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }

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

    private HttpEntity constructImageEntity(JSONObject datum) {
        String path = datum.optString("imageName");
        JSONArray sizes = datum.optJSONArray("imageSize");

        // loopj android async http support add byte[] as RequestParam item to submit multipart data
        byte[] data = ImageUtil.compressToByteArray(path);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        ByteArrayBody fileBody = new ByteArrayBody(data, "abc.jpg");
        StringBody sizeBody = null;
        try {
            sizeBody = new StringBody(sizes.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        builder.addPart("file", fileBody);
        builder.addPart("size", sizeBody);

        return builder.build();
    }

    @Override
    public void onUploadProposalAudio(String data) {
//        if (msg_type == ProposalMediaType.AUDIO)
//            this.duration = MediaPlayer.create(this, Uri.parse(data)).getDuration();
        List<String> datum = new ArrayList<>();
        datum.add(data);
        nativeUploadMultiPartFiles(datum);
    }

    /**
     * 上传文件列表
     *
     * @param filenames
     */
    private void nativeUploadMultiPartFiles(List<String> filenames) {
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

    private void nativeUploadImageFiles(String params, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        JSONArray filePaths;
        JSONArray sizes;

        try {

            if (!params.contains("imgs")) {
                JSONArray size = new JSONArray();
                JSONArray array = new JSONArray(params);
                JSONObject param = new JSONObject();
                param.put("imageName",array.getString(0));
                param.put("imageSize",size);
                HttpEntity entity = constructImageEntity(param);
                uploadFile(entity,jsCallback);

            } else {

                JSONObject object = new JSONObject(params);

                filePaths = object.optJSONArray("imgs");
                sizes = object.optJSONArray("size");

                for (int i = 0; i < filePaths.length(); i++) {
                    JSONObject param = new JSONObject();
                    param.put("imageName", filePaths.get(i).toString());
                    param.put("imageSize", sizes);

                    HttpEntity entity = constructImageEntity(param);
                    // TODO: 16/7/15 use queue to upload files
                    uploadFile(entity, jsCallback);

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile(HttpEntity entity, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        WebService.getInstance().postMultiPartFormImageFile(entity, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                JSONArray ret = new JSONArray();
                ret.put(response);

                jsCallback.callback(ret.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);

                JSONArray ret = new JSONArray();
                ret.put(errorResponse);

                jsCallback.callback(ret.toString());
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
        RestClient.getInstance().get(url, AuthHelper.getInstance().authTokenJsonHeader(), responseHandler);
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

    /**
     * load from url
     */
    public void loadImageThumbnail(ImageView iv, String imageUrl, int dimension) {
        ImageUtil.loadImageThumbnail(iv, imageUrl, dimension);
    }


    // region FullScreenImageGalleryAdapter.FullScreenImageLoader
    public void loadFullScreenImage(final ImageView iv, String imageUrl, int width, final LinearLayout bgLinearLayout) {
        ImageUtil.loadFullScreenImage(iv, imageUrl, width, bgLinearLayout);
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
