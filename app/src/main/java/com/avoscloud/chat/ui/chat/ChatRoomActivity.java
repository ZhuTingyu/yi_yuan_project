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
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avoscloud.chat.entity.AVIMUserInfoMessage;
import com.avoscloud.chat.model.AVIMHouseInfoMessage;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationChangeEvent;
import com.avoscloud.chat.service.event.FinishEvent;
import com.avoscloud.chat.ui.entry.SerializableMap;
import com.avoscloud.chat.util.Utils;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.activity.LocationHandler;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.utils.Logger;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.skeleton.R;
import com.yuan.skeleton.application.DMApplication;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by lzw on 15/4/24.
 */
public class ChatRoomActivity extends ChatActivity {
    public static final int LOCATION_REQUEST = 100;
    public static final int REQUEST_CODE_HOUSE = 101;
    private RelativeLayout chatroom;
    private LinearLayout bottomLayout;
    private SharedPreferences prefs;
    public WebViewJavascriptBridge bridge;
    private WebView webView;
    private String value;
    private LinearLayout back;

    public static void chatByConversation(Context from, AVIMConversation conv) {
        CacheService.registerConv(conv);
        ChatManager.getInstance().registerConversation(conv);
        Intent intent = new Intent(from, ChatRoomActivity.class);
        intent.putExtra(CONVID, conv.getConversationId());
        from.startActivity(intent);
    }

    public static void chatByUserId(final Activity from, String userId) {
        final ProgressDialog dialog = Utils.showSpinnerDialog(from);
        ChatManager.getInstance().fetchConversationWithUserId(userId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation conversation, AVException e) {
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
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initWebView(){
        this.webView = (WebView) findViewById(R.id.webview);
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.getSettings().setAllowFileAccess(true);
        this.webView.setWebChromeClient(new WebChromeClient());
        this.webView.setHorizontalScrollBarEnabled(false);
        this.webView.setVerticalScrollBarEnabled(false);
        this.bridge = new WebViewJavascriptBridge(this, webView, null);
        registerBridge();
        redirectToLoadUrl("agency_bbs.html");

    }

    private void registerBridge(){
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
                    layoutParams.height = 110;

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

        bridge.registerHandler("showSampleMessageBoard",new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webView.getLayoutParams();
                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                layoutParams.height = Integer.parseInt(params.get("height_s"));
                layoutParams.height = 110;
                webView.setLayoutParams(layoutParams);
            }
        });

        bridge.registerHandler("showHalfMessageBoard",new WebViewJavascriptBridge.WVJBHandler() {

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
                layoutParams.height = ((int)((height+10) * getResources().getDisplayMetrics().density));
//                    layoutParams.height = 350;
                webView.setLayoutParams(layoutParams);
                bottomLayout.setVisibility(View.VISIBLE);
            }
        });

        bridge.registerHandler("showFullMessageBoard",new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) webView.getLayoutParams();

                DisplayMetrics dm = new DisplayMetrics();//获取当前显示的界面大小
                getWindowManager().getDefaultDisplay().getMetrics(dm);

                layoutParams.height = dm.heightPixels;

//                    layoutParams.height = 200;
                webView.setLayoutParams(layoutParams);
                bottomLayout.setVisibility(View.INVISIBLE);
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
        if (StringUtil.isValidHTTPUrl(url)) {
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

    @Override
    protected void openHouseInfo() {
        Intent intent = new Intent(this,HouseInfosActivity.class);
        startActivityForResult(intent,REQUEST_CODE_HOUSE);
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

    private void testSendCustomMessage() {
        AVIMUserInfoMessage userInfoMessage = new AVIMUserInfoMessage();
        Map<String, Object> map = new HashMap<>();
        map.put("nickname", "lzwjava");
        userInfoMessage.setAttrs(map);
        conversation.sendMessage(userInfoMessage, new AVIMConversationCallback() {
            @Override
            public void done(AVException e) {
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
                    AVIMHouseInfoMessage message = new AVIMHouseInfoMessage();
                    message.setAttrs(map);
                    conversation.sendMessage(message, new AVIMConversationCallback() {
                        @Override
                        public void done(AVException e) {
                            if (e != null) {
                                Logger.d(e.getMessage());
                            }
                        }
                    });
                    break;
            }
        }

    }
}
