package com.avoscloud.chat.ui.chat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avoscloud.chat.entity.AVIMUserInfoMessage;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationChangeEvent;
import com.avoscloud.chat.service.event.FinishEvent;
import com.avoscloud.chat.ui.entry.SerializableMap;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.model.AVIMHouseInfoMessage;
import com.avoscloud.leanchatlib.utils.Logger;
import com.bugtags.library.Bugtags;
import com.dimo.http.RestClient;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.skeleton.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.common.Constants;
import com.yuan.house.utils.JsonParse;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;
import timber.log.Timber;

/**
 * Created by lzw on 15/4/24.
 */
public class ChatRoomActivity extends ChatActivity {
    public static final int LOCATION_REQUEST = 100;
    public static final int REQUEST_CODE_HOUSE = 101;
    private static SharedPreferences prefs;
    private RelativeLayout chatroom;
    private LinearLayout bottomLayout;
    private WebView webView;
    private String value;
    private LinearLayout back;
    private List<Map<String, Object>> houseInfos;
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

    public static void chatByConversation(Context from, AVIMConversation conv) {
        CacheService.registerConv(conv);
        ChatManager.getInstance().registerConversation(conv);
        Intent intent = new Intent(from, ChatRoomActivity.class);
        intent.putExtra(CONVID, conv.getConversationId());
        from.startActivity(intent);

    }

    public static void chatByUserId(final Activity from, String userId) {
        final ProgressDialog dialog = Utils.showSpinnerDialog(from);
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(from);
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
        chatroom = (RelativeLayout) findViewById(R.id.rl_chatroom);
        bottomLayout = (LinearLayout) findViewById(R.id.bottomLayout);
        back = (LinearLayout) findViewById(R.id.back);
//        initLocation();
        initWebView();
        initHouseInfos();
        gestureDetector = new GestureDetector(this, onGestureListener);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initWebView() {
        this.webView = (WebView) findViewById(R.id.webview);
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.getSettings().setAllowFileAccess(true);
        this.webView.setWebChromeClient(new WebChromeClient());
        this.webView.setHorizontalScrollBarEnabled(false);
        this.webView.setVerticalScrollBarEnabled(false);
        this.webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        this.webView.setOnTouchListener(new View.OnTouchListener() {
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
        this.bridge = new WebViewJavascriptBridge(this, webView, null);
        registerBridge();
        try {
            if (JsonParse.getInstance().judgeUserType())
                redirectToLoadUrl("user_bbs.html");
            else
                redirectToLoadUrl("agency_bbs.html");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void registerBridge() {
        bridge.registerHandler("setData", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                Timber.v("setData got:" + data);
                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);

                    String key = params.get("key");
                    value = params.get("value");

                    if (value == null || value.equals("null"))
                        prefs.edit().remove(key).commit();
                    else
                        prefs.edit().putString(key, value).commit();

                    if (null != callback) {
                        callback.callback(null);
                    }

                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webView.getLayoutParams();
                    params = StringUtil.JSONString2HashMap(value);
//                    layoutParams.height = Integer.parseInt(params.get("height_s"));
                    layoutParams.height = 130;

//                    DisplayMetrics dm = new DisplayMetrics();//获取当前显示的界面大小
//                    getWindowManager().getDefaultDisplay().getMetrics(dm);
//
//                    layoutParams.height = dm.heightPixels;
                    webView.setLayoutParams(layoutParams);
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

        bridge.registerHandler("showSampleMessageBoard", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webView.getLayoutParams();
                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                layoutParams.height = 130;
                webView.setLayoutParams(layoutParams);
            }
        });

        bridge.registerHandler("showHalfMessageBoard", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webView.getLayoutParams();
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
        });

        bridge.registerHandler("showFullMessageBoard", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webView.getLayoutParams();

                DisplayMetrics dm = new DisplayMetrics();//获取当前显示的界面大小
                getWindowManager().getDefaultDisplay().getMetrics(dm);

                layoutParams.height = dm.heightPixels;

                webView.setLayoutParams(layoutParams);
                bottomLayout.setVisibility(View.INVISIBLE);
            }
        });

        bridge.registerHandler("webChangeHouse", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Map<String, Object> map = null;
                for (int i = 0; i < houseInfos.size(); i++) {
                    map = houseInfos.get(i);

                    String houseId = null;
                    if (map.get("houseId") != null) {
                        houseId = map.get("houseId").toString();
                    }

                    if (data.equals(houseId)) return;
                }

                if (map == null) return;

                List<String> images = JSON.parseObject(map.get("images").toString(), List.class);

                AVIMHouseInfoMessage message = new AVIMHouseInfoMessage();
                message.setHouseName(map.get("estate_name").toString());
                message.setHouseAddress(map.get("location_text").toString());
                message.setHouseImage(images.get(0).toString());
                message.setAttrs(map);

                messageAgent.sendHouse(message);

            }
        });

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
    }

    public void redirectToLoadUrl(String url) {
        String mUrl = "";
        if (webView == null) {
            return;
        }
        String htmlExtractedFolder = DMApplication.getInstance().getHtmlExtractedFolder();
        mUrl = htmlExtractedFolder + "/pages/" + url;
        Timber.i("URL - " + mUrl);
        if (StringUtil.isValidHTTPUrl(mUrl)) {
            webView.loadUrl(mUrl);
        } else {
            webView.loadUrl("file:///" + mUrl);
        }
    }

    /*
    private void initLocation() {
        addLocationBtn.setVisibility(View.VISIBLE);
        setLocationHandler(new LocationHandler() {
            @Override
            public void onAddLocationButtonClicked(Activity activity) {
                LocationActivity.startToSelectLocationForResult(activity, LOCATION_REQUEST);
            }

            @Override
            public void onLocationMessageViewClicked(Activity activity, AVIMLocationMessage locationMessage) {
                LocationActivity.startToSeeLocationDetail(activity, locationMessage.getLocation().getLatitude(),
                        locationMessage.getLocation().getLongitude());
            }
        });
    }*/

    private void initHouseInfos() {
        String json = prefs.getString("userLogin", null);
        Log.i("json", json);
        String userId = getUserId(json);
        String token = getToken(json);
        String url = null;
        if (isUserLogin(json))
            url = Constants.kWebServiceSwitchable + userId + "/" + prefs.getString("target_id", null);
        else
            url = Constants.kWebServiceSwitchable + prefs.getString("target_id", null) + "/" + userId;

        OkHttpUtils.get().url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("token", token)
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onBefore(Request request) {
                        super.onBefore(request);
                        Log.i("onBefore", "==================================================");
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Log.i("onError", e.getMessage());
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.i("onResponse", response);
                        List<Map<String, Object>> list = JSON.parseObject(response, new TypeReference<List<Map<String, Object>>>() {
                        });
                        houseInfos = new ArrayList<>();
                        for (int i = 0; i < list.size(); i++) {
                            Map<String, Object> objectMap = list.get(i);
                            Map<String, Object> houseMap = JSON.parseObject(objectMap.get("house_info").toString(), new TypeReference<Map<String, Object>>() {
                            });
                            houseInfos.add(houseMap);
                        }
                        Log.i("houseInfos", houseInfos.toString());

                    }
                });
    }

    private boolean isUserLogin(String json) {
        HashMap<String, String> params = null;
        try {
            params = StringUtil.JSONString2HashMap(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (params.get("user_info") != null)
            return true;
        else
            return false;
    }

    private String getToken(String json) {
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            return params.get("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUserId(String json) {
        try {
            HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
            if (params.get("user_info") != null)
                params = StringUtil.JSONString2HashMap(params.get("user_info"));
            else
                params = StringUtil.JSONString2HashMap(params.get("agency_info"));

            return params.get("user_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void openHouseInfo() {
        Intent intent = new Intent(this, HouseInfosActivity.class);
        startActivityForResult(intent, REQUEST_CODE_HOUSE);
    }

    @Override
    protected void onResume() {
        CacheService.setCurConv(conversation);
        super.onResume();
        Bugtags.onResume(this);
    }

    @Override
    protected void onDestroy() {
        CacheService.setCurConv(null);
        super.onDestroy();
        Bugtags.onPause(this);
    }

    private void testSendCustomMessage() {
        AVIMUserInfoMessage userInfoMessage = new AVIMUserInfoMessage();
        Map<String, Object> map = new HashMap<>();
        map.put("nickname", "lzwjava");
        userInfoMessage.setAttrs(map);
        conversation.sendMessage(userInfoMessage, new AVIMConversationCallback() {
            @Override
            public void done(AVIMException e) {
                if (e != null) {
                    Logger.d(e.getMessage());
                }
            }
        });
    }

    public void onEvent(ConversationChangeEvent conversationChangeEvent) {
        if (conversation != null && conversation.getConversationId().
                equals(conversationChangeEvent.getConv().getConversationId())) {
            this.conversation = conversationChangeEvent.getConv();
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(ConversationHelper.titleOfConv(this.conversation));
        }
    }

    public void onEvent(FinishEvent finishEvent) {
        this.finish();
    }

//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    MenuInflater inflater = getMenuInflater();
//    inflater.inflate(R.menu.chat_ativity_menu, menu);
//    return super.onCreateOptionsMenu(menu);
//  }
//
//  @Override
//  public boolean onMenuItemSelected(int featureId, MenuItem item) {
//    int menuId = item.getItemId();
//    if (menuId == R.id.people) {
//      Utils.goActivity(ctx, ConversationDetailActivity.class);
//    }
//    return super.onMenuItemSelected(featureId, item);
//  }

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
                        toast(R.string.chat_cannotGetYourAddressInfo);
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

                    messageAgent.sendHouse(message);

                    bridge.callHandler("nativeChangeHouse", map.get("id"));

                    break;
            }
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //注：回调 3
        Bugtags.onDispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }
}
