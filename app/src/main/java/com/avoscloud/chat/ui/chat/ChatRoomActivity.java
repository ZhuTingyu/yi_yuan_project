package com.avoscloud.chat.ui.chat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationChangeEvent;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.model.AVIMHouseMessage;
import com.dimo.utils.DateUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.yuan.house.HouseMessageType;
import com.yuan.house.R;
import com.yuan.house.activities.SwitchHouseActivity;
import com.yuan.house.common.Constants;
import com.yuan.house.event.BridgeCallbackEvent;
import com.yuan.house.event.NotificationEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.ui.fragment.FragmentBBS;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.utils.ToastUtil;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by lzw on 15/4/24.
 */
public class ChatRoomActivity extends ChatActivity implements FragmentBBS.OnBBSInteractionListener {
    public static final int LOCATION_REQUEST = 100;
    public static final int kRequestCodeSwitchHouse = 101;

    private static SharedPreferences prefs;
    private static String leanId = "";
    String cachedHouseIdForCurrentConv;

    private FragmentBBS mFragmentBBS;
    private LinearLayout bottomLayout;
    private String value;
    private List<JSONObject> houseInfos;
    private JSONObject jsonFormatParams;
    private GestureDetector gestureDetector;
    private int mLastY = 0;
    private GestureDetector.OnGestureListener onGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {
                    float x = e2.getX() - e1.getX();
                    float y = e2.getY() - e1.getY();

                    if (x > 0) {
                        return true;
                    } else if (x < 0) {
                        return true;
                    }
                    return false;
                }
            };
    private WebView webView;
    private JSONArray jsonFormatSwitchParams;
    private String cachedHouseTradeTypeForCurrentConv;
    private InputMoreAdapter mMoreAdapter;

    public static void chatByConversation(Context from, AVIMConversation conv, JSONObject params) {
        CacheService.registerConv(conv);

        ChatManager.getInstance().registerConversation(conv);
        Intent intent = new Intent(from, ChatRoomActivity.class);
        intent.putExtra(CONVID, conv.getConversationId());
        intent.putExtra(Constants.kHouseParamsForChatRoom, params.toString());

        from.startActivity(intent);
    }

    public static void chatByConversation(Context from, AVIMConversation conv) {
        CacheService.registerConv(conv);

        ChatManager.getInstance().registerConversation(conv);
        Intent intent = new Intent(from, ChatRoomActivity.class);
        intent.putExtra(CONVID, conv.getConversationId());
        from.startActivity(intent);
    }

    /**
     * Deprecated
     */
    public static void chatByUserId(final Activity from, String userId) {
        leanId = userId;

        final ProgressDialog dialog = Utils.showSpinnerDialog(from);
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(from);

        String houseId = prefs.getString("houseId", null);
        String auditType = prefs.getString("auditType", null);

        StringBuffer sb = new StringBuffer();
        if (auditType == null) {
            sb.append(houseId);
        } else {
            sb.append("000");
            sb.append(auditType);
            sb.append(houseId);
        }

        ChatManager.getInstance().fetchConversationWithUserId(sb.toString(), leanId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation conversation, AVIMException e) {
                dialog.dismiss();
                if (Utils.filterException(e)) {
                    chatByConversation(from, conversation);
                }
            }
        });
    }

    public static void chatByUserId(final Activity from, final JSONObject params) {
        leanId = params.optString("lean_id");

        final ProgressDialog dialog = Utils.showSpinnerDialog(from);
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(from);

        ChatManager.getInstance().fetchConversationWithUserId(params, leanId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation conversation, AVIMException e) {
                dialog.dismiss();
                if (Utils.filterException(e)) {
                    chatByConversation(from, conversation, params);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String raw = bundle.getString(Constants.kHouseParamsForChatRoom);
            try {
                jsonFormatParams = new JSONObject(raw);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String peerUserName = jsonFormatParams.optString("name");
        if (!TextUtils.isEmpty(peerUserName)) {
            // 更新显示用户身份
            if ("agency".equals(jsonFormatParams.optString("type"))) {
                peerUserName += " " + getString(R.string.user_type_agency);
            } else {
                peerUserName += " " + getString(R.string.user_type_user);
            }

            setTitleItem(peerUserName);
        } else {
            requestAnonymousInfo();
        }

        setRightItem(R.drawable.btn_search, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "resources.html";

                JSONObject object = new JSONObject();
                JSONObject innerObject = new JSONObject();
                try {
                    innerObject.put("title", "全网房源");
                    innerObject.put("hasBackButton", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    object.put("params", innerObject);
                    openLinkInNewActivity(url, object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        gestureDetector = new GestureDetector(this, onGestureListener);

        mFragmentBBS = FragmentBBS.newInstance();

        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.add(R.id.fragmentBBS, mFragmentBBS, Constants.kFragmentTagBBS);
        mFragmentTransaction.commit();

        initSuggestedHouseInfos();

        contentEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendText();

                    return true;
                }
                return false;
            }
        });

        bindAdapter(jsonFormatParams);

        cachedHouseIdForCurrentConv = jsonFormatParams.optString("house_id");
        cachedHouseTradeTypeForCurrentConv = jsonFormatParams.optString("trade_type");

        mMoreAdapter = new InputMoreAdapter(this);
        mMoreAdapter.addItem("照片", -1);
        GridView gv = (GridView) findViewById(R.id.chatAddLayout);
        gv.setAdapter(mMoreAdapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String text = (String) mMoreAdapter.getItem(position);
                int value = mMoreAdapter.getValue(position);
                if (text.equals("中心合同")) {
                    getWebViewFragment().getBridge().callHandler("ClickContractButton", Integer.toString(value));
                } else if (text.equals("买卖合同")) {
                    getWebViewFragment().getBridge().callHandler("ClickContractButton", Integer.toString(value));
                } else if (text.equals("补充协议")) {
                    getWebViewFragment().getBridge().callHandler("ClickContractButton", Integer.toString(value));
                } else if (text.equals("前置留言板")) {
                    getWebViewFragment().getBridge().callHandler("onPreConditionButtonClick");
                } else if (text.equals("房源")) {
                    showSuggestedHouses();
                } else if (text.equals("照片")) {
                    selectImage();
                }
            }
        });
    }

    private void requestAnonymousInfo() {
        String userId, agencyId;

        if (AuthHelper.userAlreadyLogin() && AuthHelper.iAmUser()) {
            userId = AuthHelper.userId();
            agencyId = jsonFormatParams.optString("user_id");
        } else {
            agencyId = AuthHelper.userId();
            userId = jsonFormatParams.optString("user_id");
        }

        String url = String.format("/chat-info/user/%s/agency/%s", userId, agencyId);

        restGet(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                // TODO: 16/7/11 update activity title
                JSONObject to = response.optJSONObject("to");
                String name = to.optString("name");
                setTitleItem(name);

                String avatar = to.optString("avatar");

                // TODO: 16/7/11 update mMessageAdapter incoming avatar
                if (!TextUtils.isEmpty(avatar)) {
                    mMessageAdapter.updatePeerAvatar(avatar);
                    mMessageAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    @Override
    public void onFragmentInteraction(WebViewBaseFragment fragment) {
        super.onFragmentInteraction(fragment);

        updateWebViewSettings();
        doOtherStuff();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!TextUtils.isEmpty(cachedHouseIdForCurrentConv)) {
            prefs.edit().putString(Constants.kLastActivatedHouseId, cachedHouseIdForCurrentConv).apply();
        }
        if (!TextUtils.isEmpty(cachedHouseTradeTypeForCurrentConv)) {
            prefs.edit().putString(Constants.kLastActivatedHouseTradeType, cachedHouseTradeTypeForCurrentConv).apply();
        }
    }

    // TODO: 16/6/6 WTF ???
    private void doOtherStuff() {
        mMessageAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                if (mMessageAdapter.getDatas().size() == 0)
                    return;

                AVIMTypedMessage msg = mMessageAdapter.getDatas().get(mMessageAdapter.getDatas().size() - 1);
                HouseMessageType type = HouseMessageType.getMessageType(msg.getMessageType());

                String resultMessage = "";
                long date = 0;
                switch (type) {
                    case TextMessageType:
                        AVIMTextMessage textMsg = (AVIMTextMessage) msg;
                        date = msg.getTimestamp();
                        resultMessage = textMsg.getText();
                        break;
                    case ImageMessageType:
                        AVIMImageMessage imageMsg = (AVIMImageMessage) msg;
                        date = imageMsg.getTimestamp();
                        resultMessage = "[图片]";
                        break;
                    case AudioMessageType:
                        AVIMAudioMessage audioMessage = (AVIMAudioMessage) msg;
                        date = audioMessage.getTimestamp();
                        resultMessage = "[语音]";
                        break;
                    case LocationMessageType:
                        AVIMLocationMessage locMsg = (AVIMLocationMessage) msg;
                        date = locMsg.getTimestamp();
                        resultMessage = "[位置]";
                        break;
                    case HouseMessageType:
                        AVIMHouseMessage houseMsg = (AVIMHouseMessage) msg;
                        date = houseMsg.getTimestamp();
                        resultMessage = "[房源]";

                    default:
                        break;
                }

                String leanId = jsonFormatParams.optString("lean_id");
                String auditType = jsonFormatParams.optString("audit_type");
                String houseId = jsonFormatParams.optString("house_id");

                JSONObject object = new JSONObject();

                try {
                    object.put("date", DateUtil.toDateString(new Date(date), Constants.kDateFormatStyleShort));
                    object.put("message", resultMessage);
                    object.put("houseId", houseId);
                    object.put("leanId", leanId);
                    object.put("is_read", true);
                    object.put("audit_type", auditType);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // dispatch the event to WebViewBaseFragment
                EventBus.getDefault().post(new BridgeCallbackEvent(BridgeCallbackEvent.BridgeCallbackEventEnum.CALLBACK, object.toString()));

//                if (MessageHelper.fromMe(msg) && msg.getMessageStatus() == AVIMMessage.AVIMMessageStatus.AVIMMessageStatusSent) {
//                    // FIXME: 16/6/22 有些时候 house id 是空的
//                    ChatManager.getInstance().storeLastMessage(msg, object);
//                }
            }
        });
    }

    private void updateWebViewSettings() {
        webView = mFragmentBBS.getWebView();
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    final int y = (int) event.getY();

                    if (y < mLastY) return true;

                    mLastY = y;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) mLastY = 0;

                return false;
            }
        });
    }

    private void initSuggestedHouseInfos() {
        String url = Constants.kWebServiceSwitchable;

        if (AuthHelper.userAlreadyLogin() && AuthHelper.iAmUser()) {
            url += AuthHelper.userId() + "/" + jsonFormatParams.optString("user_id");
        } else {
            url += jsonFormatParams.optString("user_id") + "/" + AuthHelper.userId();
        }

        restGet(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);

                jsonFormatSwitchParams = response;
                houseInfos = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    JSONObject object;
                    JSONObject houseInfo;
                    try {
                        object = (JSONObject) response.get(i);
                        houseInfo = object.getJSONObject("house_info");
                        houseInfos.add(houseInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (!houseInfos.isEmpty()) {
                    mMoreAdapter.addItem("房源", -1);
                    mMoreAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    @Override
    protected void sendText() {
        String content = contentEdit.getText().toString();
        if (!TextUtils.isEmpty(content)) {
            AVIMTextMessage message = new AVIMTextMessage();

            Map<String, Object> attrs = new HashMap<>();
            attrs.put("houseId", cachedHouseIdForCurrentConv);
            attrs.put("username", jsonFormatParams.optString("nickname"));

            message.setAttrs(attrs);

            message.setText(content);

            messageAgent.sendEncapsulatedTypedMessage(message);

            contentEdit.setText("");
        }
    }

    @Override
    protected void showSuggestedHouses() {
        Intent intent = new Intent(this, SwitchHouseActivity.class);
        intent.putExtra(Constants.kHouseSwitchParamsForChatRoom, jsonFormatSwitchParams.toString());

        startActivityForResult(intent, kRequestCodeSwitchHouse);
    }

    public void onEvent(ConversationChangeEvent conversationChangeEvent) {
        String convId = conversationChangeEvent.getConv().getConversationId();
        if (conversation != null && conversation.getConversationId().equals(convId)) {
            this.conversation = conversationChangeEvent.getConv();

            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setTitle(ConversationHelper.titleOfConv(this.conversation));
            }
        }
    }

    public void onEvent(NotificationEvent event) {
        if (event.getEventType() == NotificationEvent.NotificationEventEnum.NOTICE_MESSAGE) {
            getWebViewFragment().getBridge().callHandler("MessageNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.NEW_TRANSACTION) {
            getWebViewFragment().getBridge().callHandler("userTransactionNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.NEW_AGENCY_TRANSATION) {
            getWebViewFragment().getBridge().callHandler("agencyTransactionNotification", event.getHolder());
        } else if (event.getEventType() == NotificationEvent.NotificationEventEnum.BBS_MESSAGE) {
            getWebViewFragment().getBridge().callHandler("BBSNotification", event.getHolder());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case LOCATION_REQUEST: {
                    final double latitude = data.getDoubleExtra(LocationActivity.LATITUDE, 0);

                    final double longitude = data.getDoubleExtra(LocationActivity.LONGITUDE, 0);
                    final String address = data.getStringExtra(LocationActivity.ADDRESS);
                    if (!TextUtils.isEmpty(address)) {
                        messageAgent.sendLocation(latitude, longitude, address);
                    } else {
                        ToastUtil.show(mContext, R.string.chat_cannotGetYourAddressInfo);
                    }
                    hideBottomLayout();
                    break;
                }
                case kRequestCodeSwitchHouse: {
                    String raw = data.getStringExtra(Constants.kBundleKeyAfterSwitchHouseSelected);
                    JSONObject object = null;
                    try {
                        object = switchHouse(raw);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    getWebViewFragment().getBridge().callHandler("nativeChangeHouse", object.optString("id"));
                    break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        CacheService.setCurConv(conversation);

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        CacheService.setCurConv(null);

        super.onDestroy();
    }

    @Override
    public void onSetContractButton(String data) {
        JSONArray array;
        try {
            array = new JSONArray(data);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < array.length(); i++) {
            String contractText = array.optString(i);
            mMoreAdapter.addItem(contractText, i);
        }
        mMoreAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSetPreConditionButton(String data) {
        String text = "前置留言板";
        mMoreAdapter.addItem(text, -1);
        mMoreAdapter.notifyDataSetChanged();
    }

    @Override
    public void onWebChangeHouse(String data) {
        try {
            switchHouse(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject switchHouse(String data) throws JSONException {
//        JSONObject object = null;
//        for (int i = 0; i < houseInfos.size(); i++) {
//            object = houseInfos.get(i);
//
//            String houseId = null;
//            if (object.optString("id") != null) {
//                houseId = object.optString("id");
//            }
//
//            if (data.equals(houseId)) return null;
//        }
//
//        if (object == null) return null;

        JSONObject object = new JSONObject(data);
        cachedHouseIdForCurrentConv = object.optString("id");

        JSONArray images = object.optJSONArray("images");

        AVIMHouseMessage message = new AVIMHouseMessage();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("houseName", object.optString("estate_name"));
        // TODO: 16/6/28 要考虑和匿名系统集成
        attrs.put("username", "wo");
        if (images == null || images.length() == 0) {
            attrs.put("houseImage", null);
        } else {
            attrs.put("houseImage", images.optString(0));
        }
        attrs.put("houseId", cachedHouseIdForCurrentConv);
        attrs.put("houseAddress", object.optString("location_text"));

        message.setAttrs(attrs);

        messageAgent.sendEncapsulatedTypedMessage(message);

        return object;
    }

    private JSONObject getHeightObject() {
        String bbsHeight = prefs.getString("bbs_height", null);

        JSONObject object = null;
        try {
            object = new JSONObject(bbsHeight);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }

    private int getHeightS() {
        JSONObject object = getHeightObject();
        if (object != null) {
            return object.optInt("height_s");
        }

        return 0;
    }

    private int getHeightM() {
        JSONObject object = getHeightObject();
        if (object != null) {
            return object.optInt("height_m");
        }

        return 0;
    }

    @Override
    public void onShowBBSView(boolean data) {
        if (!data) {
            findViewById(R.id.fragmentBBS).setVisibility(View.GONE);
        }
    }

    @Override
    public void onShowSampleMessageBoard() {
        int height = ((int) (getHeightS() * getResources().getDisplayMetrics().density));

        resizeBBSBoard(height);
    }

    @Override
    public void onShowHalfMessageBoard() {
        int height = ((int) (getHeightM() * getResources().getDisplayMetrics().density));

        resizeBBSBoard(height);
    }

    @Override
    public void onShowFullMessageBoard() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        resizeBBSBoard(dm.heightPixels);
    }

    @Override
    public void onGetFirstHouseInfo(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
        // 记录上一次的房源 id / trade_type，如果 web 没有传 house_id / trade_type 进聊天，则告诉 web 房源 id。
        // 如果没有上一次，就传可切换房源的第一条。
        JSONObject object = new JSONObject();

        String id = cachedHouseIdForCurrentConv, tradeType = cachedHouseTradeTypeForCurrentConv;

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(tradeType)) {
            id = prefs.getString(Constants.kLastActivatedHouseId, null);
            tradeType = prefs.getString(Constants.kLastActivatedHouseTradeType, null);

            if (TextUtils.isEmpty(id) && houseInfos != null) {
                id = houseInfos.get(0).optString("id");
                tradeType = houseInfos.get(0).optString("trade_type");
            }
        }

        try {
            object.put("id", id);
            object.put("trade_type", tradeType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (null != callback) {
            callback.callback(object);
        }
    }

    private void resizeBBSBoard(int height) {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fragmentBBS);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.height = height;

        frameLayout.setLayoutParams(layoutParams);
    }
}
