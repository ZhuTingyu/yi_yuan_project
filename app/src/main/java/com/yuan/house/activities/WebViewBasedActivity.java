package com.yuan.house.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.util.Log;
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
import com.avoscloud.chat.ui.chat.GroupChatActivity;
import com.avoscloud.chat.ui.chat.ServiceChatActivity;
import com.avoscloud.chat.ui.chat.SingleChatActivity;
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
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
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
import com.yuan.house.payment.AliPay;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.FileUtil;
import com.yuan.house.utils.ImageUtil;
import com.yuan.house.utils.SystemServiceUtil;
import com.yuan.house.utils.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.StringCallback;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import me.nereo.multi_image_selector.MultiImageSelector;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;
import okhttp3.Call;
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
    private int indexOfImageInUploadQueue = 0;
    private JSONArray responseOfImageUploadTask;

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
        getWebViewFragment().getBridge().callHandler("onBroadcast", event.getPayload());
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

            Timber.w("KICK_OUT : Online token - %s ; Local token - %s", object.optString("exclusive_token"), AuthHelper.getInstance().getUserToken());

            if (object == null ||
                    (AuthHelper.getInstance().userAlreadyLogin() &&
                            AuthHelper.getInstance().getUserId().equals(object.optString("user_id"))
                            && !AuthHelper.getInstance().getUserToken().equals(object.optString("exclusive_token"))
                    )) {
                Toast.makeText(mContext, "您的账号在别处登陆", Toast.LENGTH_SHORT).show();

                DMApplication.getInstance().kickOut();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int kActivityRequestCodeImageCrop = 14;
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
            Timber.v("kActivityRequestCodeImagePickThenUpload");
            List<String> files = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            Fragment fragment = getFragment(Constants.kFragmentTagProposal);
            if (fragment != null) {
                ProposalFragment pfragment = (ProposalFragment) fragment;
                for (String fileName : files) {
                    pfragment.uploadFile(fileName);
                }
            }
        } else if (requestCode == Constants.kActivityRequestCodeImagePickThenCropRectangle
                || requestCode == Constants.kActivityRequestCodeImagePickThenCropSquare) {
            Timber.v("kActivityRequestCodeImagePickThenCropRectangle");
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

    public void onBridgeStartSingleChat(JSONObject object) {
        SingleChatActivity.chatByUserId(this, object);
    }

    public void onBridgeStartGroupChat(JSONObject object) {
        Timber.v("onBridgeStartGroupChat");

        GroupChatActivity.chatByUserIds(this, object);
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
                        if (jsCallback != null) {
                            jsCallback.callback(index);
                        }

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

                    if (jsCallback != null) {
                        jsCallback.callback(array.toString());
                    }
                }
            }
        });
    }

    private void sendHouseInfoMessage(String peerId, final JSONObject info) {
        ChatManager.getInstance().fetchConversationWithUserId(info, peerId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation avimConversation, AVIMException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

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
            JSONObject user = userArray.optJSONObject(i);
            sendHouseInfoMessage(user.optString("lean_id"), houseInfo);
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

                    if (avimConversation != null) {
                        MessageAgent messageAgent = new MessageAgent(avimConversation);
                        messageAgent.sendEncapsulatedTypedMessage(message);

                        // 发送成功之后需要缓存该条消息到本地
                        ChatManager.getInstance().storeLastMessage(message);
                    }

                    if (finalI == (finalLeanIdList.length() - 1)) {
                        if (callback != null) {
                            callback.callback("onBridgeSendNoticeMessage finished");
                        }
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

    public void onBridgeHideRightItem() {
        hideRightItem();
    }

    public void onBridgeSetRightItem(int resourceId, View.OnClickListener onRightItemClick) {
        setRightItem(resourceId, onRightItemClick);
    }

    public void onBridgeSetRightItem(String text, View.OnClickListener onRightItemClick) {
        setRightItem(text, onRightItemClick);
    }

    public void onBridgeUploadFiles(String datum, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        // upload from first image
        indexOfImageInUploadQueue = 0;
        responseOfImageUploadTask = new JSONArray();

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

                locationClient.stop();
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

    private void nativeUploadImageFiles(String params, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        JSONArray filePaths;
        JSONArray sizes = new JSONArray();

        try {
            if (!params.contains("imgs")) {
                filePaths = new JSONArray(params);
            } else {
                // 上传其他图片
                JSONObject object = new JSONObject(params);

                filePaths = object.optJSONArray("imgs");
                sizes = object.optJSONArray("size");
            }

            String pack = sizes == null ? null : sizes.toString();
            uploadFiles(filePaths, pack, jsCallback);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // upload multi images one by one, halt and prompt if any file failed to upload
    private void uploadFiles(final JSONArray filePaths, final String size, final WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
        String path = filePaths.optString(indexOfImageInUploadQueue);

        PostFormBuilder builder = OkHttpUtils.post().url(Constants.kWebServiceImageUpload)
                .addHeader(Constants.kHttpReqKeyContentType, "multipart/form-data")
                .addHeader(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken())
                .addFile("file", path, new File(path));

        if (!TextUtils.isEmpty(size)) {
            builder.addParams("size", size);
        }

        builder.build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        e.printStackTrace();

                        ToastUtil.showShort(mContext, R.string.error_failed_to_upload_image);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Timber.v("Upload Image : " + response);

                        try {
                            JSONObject object = new JSONObject(response);
                            responseOfImageUploadTask.put(object);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (indexOfImageInUploadQueue < filePaths.length() - 1) {
                            indexOfImageInUploadQueue++;

                            uploadFiles(filePaths, size, jsCallback);
                        } else {
                            if (jsCallback != null) {
                                jsCallback.callback(responseOfImageUploadTask.toString());
                            }
                        }
                    }
                });
    }

    @Override
    public void onBridgeStartServiceChat() {
        RestClient.getInstance().get("/service-conversation", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                boolean isOpen = Integer.parseInt(response.optString("session_id")) != 0;
                ServiceChatActivity.chatByConversation(mContext, response.optString("conversation_id"), isOpen);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
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

    protected void executeAppVersionCheck() {
        boolean hasNewVersion = prefs.getString(Constants.kAppHasNewVersion, "0").equals("1");

        if (hasNewVersion) {
            AlertDialog.Builder builder = new AlertDialog.Builder(WebViewBasedActivity.this);
            builder.setTitle("新版本");
            builder.setMessage("检测到有新版本, 是否立即更新?");

            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadFileAndPrepareInstallation();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            if (!isFinishing()) {
                builder.create().show();
            }
        }
    }
    // endregion

    private void downloadFileAndPrepareInstallation() {
        String url = prefs.getString(Constants.kNewAppDownloadUrl, null);
        //String url = "http://apk.hiapk.com/appdown/com.hiapk.live?planid=2879546";
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(mContext, "升级程序异常,请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dailog = new ProgressDialog(mContext);
        dailog.setTitle(R.string.txt_downloading);
        dailog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dailog.setMax(100);
        if (!isFinishing()) {
            dailog.show();
        }

        RestClient.getInstance().get(url, null, new FileAsyncHttpResponseHandler(getApplicationContext()) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Log.e("down", throwable.toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                file.setReadable(true, false);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                mContext.startActivity(intent);
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                double por = (bytesWritten * 1.0 / totalSize) * 100;
                dailog.setProgress((int) por);
                if (bytesWritten == totalSize) {
                    dailog.dismiss();
                }
                super.onProgress(bytesWritten, totalSize);
            }
        });

    }
    // endregion
}
