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
import com.avoscloud.chat.ui.entry.SerializableMap;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.model.AVIMHouseInfoMessage;
import com.dimo.utils.StringUtil;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.yuan.house.common.Constants;
import com.yuan.house.ui.fragment.FragmentBBS;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.utils.ToastUtil;
import com.yuan.skeleton.R;

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
    public static final int REQUEST_CODE_HOUSE = 101;
    private static SharedPreferences prefs;
    private static String leanId = "";
    private FragmentBBS mFragmentBBS;
    private RelativeLayout chatroom;
    private LinearLayout bottomLayout;
    private String value;
    private List<JSONObject> houseInfos;
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

        ChatManager.getInstance().fetchConversationWithUserId(sb.toString(), userId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation conversation, AVIMException e) {
                dialog.dismiss();
                if (Utils.filterException(e)) {
                    chatByConversation(from, conversation);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        gestureDetector = new GestureDetector(this, onGestureListener);

        mFragmentBBS = FragmentBBS.newInstance();

        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.add(R.id.fragmentBBS, mFragmentBBS, Constants.kFragmentTagBBS);
        mFragmentTransaction.commit();

        initHouseInfos();
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
                    if (y < mLastY)
                        return true;
                    mLastY = y;
                }

                if (event.getAction() == MotionEvent.ACTION_UP)
                    mLastY = 0;

                return false;
            }
        });
    }

    private void initHouseInfos() {
        String json = prefs.getString(Constants.kWebDataKeyUserLogin, null);
        String userId = getUserId(json);
        String url = Constants.kWebServiceSwitchable;

        if (userAlreadyLogin(json)) {
            url += userId + "/" + prefs.getString("target_id", null);
        } else {
            url += prefs.getString("target_id", null) + "/" + userId;
        }

        restGet(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);

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
    protected void openHouseInfo() {
        Intent intent = new Intent(this, HouseInfosActivity.class);
        startActivityForResult(intent, REQUEST_CODE_HOUSE);
    }

    public void onEvent(ConversationChangeEvent conversationChangeEvent) {
        if (conversation != null && conversation.getConversationId().
                equals(conversationChangeEvent.getConv().getConversationId())) {
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
                case LOCATION_REQUEST:
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
                case REQUEST_CODE_HOUSE:
                    SerializableMap serializableMap = (SerializableMap) data.getSerializableExtra("data");
                    Map<String, Object> map = serializableMap.getMap();
                    List<String> images = JSON.parseObject(map.get("images").toString(), List.class);

                    AVIMHouseInfoMessage message = new AVIMHouseInfoMessage();
                    message.setHouseName(map.get("estate_name").toString());
                    message.setHouseAddress(map.get("location_text").toString());
                    message.setHouseImage(images.get(0).toString());
                    message.setAttrs(map);

                    messageAgent.sendEncapsulatedTypedMessage(message);

//                    bridge.callHandler("nativeChangeHouse", map.get("id"));

                    break;
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

        Map<String, Object> houseInfo = JSON.parseObject(object.toString(), new TypeReference<Map<String, Object>>() {
        });

        JSONArray jsonImages = object.optJSONArray("images");
        ArrayList<String> images = new ArrayList<>();

        if (jsonImages != null) {
            for (int i = 0; i < jsonImages.length(); i++) {
                images.add(jsonImages.optString(i));
            }

            AVIMHouseInfoMessage message = new AVIMHouseInfoMessage();
            message.setHouseName(object.optString("estate_name").toString());
            message.setHouseAddress(object.optString("location_text").toString());
            message.setHouseImage(images.get(0).toString());
            message.setAttrs(houseInfo);

            messageAgent.sendEncapsulatedTypedMessage(message);
        }
    }

    @Override
    public void onShowSampleMessageBoard() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) webView.getLayoutParams();
        HashMap<String, String> params = null;
        try {
            params = StringUtil.JSONString2HashMap(value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        layoutParams.height = 130;
        webView.setLayoutParams(layoutParams);
    }

    @Override
    public void onShowHalfMessageBoard() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) webView.getLayoutParams();
        HashMap<String, String> params = null;
        try {
            params = StringUtil.JSONString2HashMap(value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int height = Integer.parseInt(params.get("height_m"));
        layoutParams.height = ((int) ((height + 10) * getResources().getDisplayMetrics().density));
        webView.setLayoutParams(layoutParams);
        bottomLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onShowFullMessageBoard() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) webView.getLayoutParams();

        DisplayMetrics dm = new DisplayMetrics();//获取当前显示的界面大小
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        layoutParams.height = dm.heightPixels;

        webView.setLayoutParams(layoutParams);
        bottomLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onConfigBBSHeight(int height) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) webView.getLayoutParams();

        // 获取当前显示的界面大小
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        layoutParams.height = (int) (height * dm.density);

        webView.setLayoutParams(layoutParams);
    }
}
