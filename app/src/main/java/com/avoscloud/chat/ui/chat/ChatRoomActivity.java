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
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMReservedMessageType;
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
import com.avoscloud.leanchatlib.model.AVIMHouseInfoMessage;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.yuan.house.R;
import com.yuan.house.activities.SwitchHouseActivity;
import com.yuan.house.common.Constants;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.ui.fragment.FragmentBBS;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.utils.ToastUtil;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lzw on 15/4/24.
 */
public class ChatRoomActivity extends ChatActivity implements FragmentBBS.OnBBSInteractionListener {
    public static final int LOCATION_REQUEST = 100;
    public static final int kRequestCodeSwitchHouse = 101;

    private static SharedPreferences prefs;
    private static String leanId = "";
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
        String userId = params.optString("user_id");

        leanId = params.optString("lean_id");

        final ProgressDialog dialog = Utils.showSpinnerDialog(from);
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(from);

        String houseId = params.optString("house_id");
        String auditType = params.optString("audit_type");

        StringBuilder sb = new StringBuilder();
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

        setTitleItem(jsonFormatParams.optString("user_id"));

        gestureDetector = new GestureDetector(this, onGestureListener);

        mFragmentBBS = FragmentBBS.newInstance();

        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.add(R.id.fragmentBBS, mFragmentBBS, Constants.kFragmentTagBBS);
        mFragmentTransaction.commit();

        initSuggestedHouseInfos();
    }

    @Override
    public void onFragmentInteraction(WebViewBaseFragment fragment) {
        super.onFragmentInteraction(fragment);

        updateWebViewSettings();
        doOtherStuff();
    }

    // TODO: 16/6/6 WTF ???
    private void doOtherStuff() {
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                if (adapter.getDatas().size() == 0)
                    return;

                AVIMTypedMessage msg = adapter.getDatas().get(adapter.getDatas().size() - 1);
                AVIMReservedMessageType type = AVIMReservedMessageType.getAVIMReservedMessageType(msg.getMessageType());

                String resultMessage = "";
                long date = 0;
                switch (type) {
                    case TextMessageType:
                        AVIMTextMessage textMsg = (AVIMTextMessage) msg;
                        ToastUtil.showShort(getApplicationContext(), textMsg.getText());
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
                    default:
                        break;
                }

                Map<String, Object> params = new HashMap<>();
                params.put("date", date);
                params.put("message", resultMessage);
                params.put("houseId", prefs.getString("houseId", null));
                params.put("leanId", leanId);
                params.put("is_read", 1);
                params.put("auditType", prefs.getString("auditType", null));

                String json = com.alibaba.fastjson.JSONObject.toJSONString(params);
                mFragmentBBS.getBridge().callHandler("onLastMessageChangeByHouse", json);
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
                        object = (JSONObject) response.get(0);
                        houseInfo = object.getJSONObject("house_info");
                        houseInfos.add(houseInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
            attrs.put("houseId", jsonFormatParams.optString("house_id"));
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
            actionBar.setTitle(ConversationHelper.titleOfConv(this.conversation));
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
                    JSONObject object;
                    try {
                        object = new JSONObject(raw);

                        JSONArray images = object.optJSONArray("images");

                        AVIMHouseInfoMessage message = new AVIMHouseInfoMessage();
                        message.setHouseName(object.optString("estate_name"));
                        message.setHouseAddress(object.optString("location_text"));
                        if (images != null) {
                            message.setHouseImage(images.optString(0));
                        }

                        Map<String, Object> attrs = JSON.parseObject(raw, new TypeReference<Map<String, Object>>() {
                        });
                        message.setAttrs(attrs);

                        messageAgent.sendEncapsulatedTypedMessage(message);
                        getWebViewFragment().getBridge().callHandler("nativeChangeHouse", object.getString("id"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
        findViewById(R.id.btnContract).setVisibility(View.VISIBLE);
    }

    @Override
    public void onSetPreConditionButton(String data) {
        findViewById(R.id.btnPrecondition).setVisibility(View.VISIBLE);
    }

    @Override
    public void onWebChangeHouse(String data) {
        JSONObject object = null;
        for (int i = 0; i < houseInfos.size(); i++) {
            object = houseInfos.get(i);

            String houseId = null;
            if (object.optString("id") != null) {
                houseId = object.optString("id");
            }

            if (data.equals(houseId)) return;
        }

        if (object == null) return;

        JSONArray images = object.optJSONArray("images");

        AVIMHouseInfoMessage message = new AVIMHouseInfoMessage();
        message.setHouseName(object.optString("estate_name"));
        message.setHouseAddress(object.optString("location_text"));
        message.setHouseImage(images.optString(0));

        Map<String, Object> attrs = JSON.parseObject(jsonFormatParams.toString(), new TypeReference<Map<String, Object>>() {
        });
        message.setAttrs(attrs);

        messageAgent.sendEncapsulatedTypedMessage(message);
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

    private void resizeBBSBoard(int height) {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fragmentBBS);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.height = height;

        frameLayout.setLayoutParams(layoutParams);
    }
}
