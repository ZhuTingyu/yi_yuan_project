package com.avoscloud.chat.ui.chat;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;

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
import com.avoscloud.leanchatlib.adapter.ChatMessageAdapter;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.controller.MessageAgent;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.model.AVIMHouseMessage;
import com.avoscloud.leanchatlib.model.AVIMPresenceMessage;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.model.MessageEvent;
import com.avoscloud.leanchatlib.utils.DownloadUtils;
import com.avoscloud.leanchatlib.utils.NetAsyncTask;
import com.avoscloud.leanchatlib.view.xlist.XListView;
import com.dimo.web.WebViewJavascriptBridge;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.yuan.house.R;
import com.yuan.house.activities.SwitchHouseActivity;
import com.yuan.house.adapter.InputMoreAdapter;
import com.yuan.house.common.Constants;
import com.yuan.house.event.BridgeCallbackEvent;
import com.yuan.house.event.NotificationEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.ui.fragment.FragmentBBS;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.utils.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import timber.log.Timber;

/**
 * Created by lzw on 15/4/24.
 */
public class SingleChatActivity extends ChatActivity implements FragmentBBS.OnBBSInteractionListener,
        XListView.IXListViewListener {
    public static final int LOCATION_REQUEST = 100;
    public static final int kRequestCodeSwitchHouse = 101;
    private static final int PAGE_SIZE = 20;
    private static SharedPreferences prefs;
    private static String currentChattingConvid;
    protected ChatMessageAdapter mMessageAdapter;
    protected MessageAgent.SendCallback defaultSendCallback = new DefaultSendCallback();
    String cachedHouseIdForCurrentConv;
    private MessageAgent messageAgent;
    private String mChatMessage;            //需要转发或复制的信息
    private FragmentBBS mFragmentBBS;
    private List<JSONObject> houseInfos;
    private AVIMConversation conversation;
    private ConversationType conversationType;
    private JSONObject jsonFormatParams;
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
    private JSONArray jsonFormatSwitchParams;
    private String cachedHouseTradeTypeForCurrentConv;
    private InputMoreAdapter mMoreAdapter;
    private ScheduledExecutorService scheduledExecutorServiceForPresence;
    private ScheduledExecutorService scheduledExecutorServiceForPresenceCheckInSeconds;
    private int mHeartBeatTimesForRemainLive;

    public static String getCurrentChattingConvid() {
        return currentChattingConvid;
    }

    public static void setCurrentChattingConvid(String currentChattingConvid) {
        SingleChatActivity.currentChattingConvid = currentChattingConvid;
    }

    public static void chatByConversation(Context from, AVIMConversation conv, JSONObject params) {
        CacheService.registerConv(conv);

        ChatManager.getInstance().registerConversation(conv);
        Intent intent = new Intent(from, SingleChatActivity.class);
        intent.putExtra(CONVID, conv.getConversationId());
        intent.putExtra(Constants.kHouseParamsForChatRoom, params.toString());

        from.startActivity(intent);
    }

    public static void chatByUserId(final Context from, final JSONObject params) {
        String leanId = params.optString("lean_id");

        final ProgressDialog dialog = Utils.showSpinnerDialog(from);
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(from);

        ChatManager.getInstance().fetchConversationWithUserId(params, leanId, new AVIMConversationCreatedCallback() {
            @Override
            public void done(AVIMConversation conversation, AVIMException e) {
                dialog.dismiss();
                if (Utils.filterException(e)) {
                    Timber.w("FETCH CONV - CONV Id : %s", conversation.getConversationId());

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
                String url = "resources.html?history";

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


        GestureDetector gestureDetector = new GestureDetector(this, onGestureListener);

        mFragmentBBS = FragmentBBS.newInstance();

        mFragmentTransaction = mFragmentManager.beginTransaction();
        mFragmentTransaction.add(R.id.fragmentBBS, mFragmentBBS, Constants.kFragmentTagBBS);
        mFragmentTransaction.commit();

        initListView();

        initByIntent(getIntent());

        initSuggestedHouseInfos();

        sendPresenceMessage();

        setupPresenceGuardian();

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

                // 中心合同一定是0，买卖合同一定是1，补充协议一定是2
                if (text.equals("中心合同")) {
                    getWebViewFragment().getBridge().callHandler("ClickContractButton", 0);
                } else if (text.equals("买卖合同")) {
                    getWebViewFragment().getBridge().callHandler("ClickContractButton", 1);
                } else if (text.equals("补充协议")) {
                    getWebViewFragment().getBridge().callHandler("ClickContractButton", 2);
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

    @Override
    protected void onPause() {
        super.onPause();

        setCurrentChattingConvid(null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_item_longclick_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_retransmission:
                ToastUtil.showShort(mContext, "转发" + mChatMessage);
                return true;
            case R.id.menu_copy:
                ToastUtil.showShort(mContext, "复制" + mChatMessage);
                return true;
            case R.id.menu_more:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void sendAudio(String audioPath) {
        if (messageAgent != null) {
            messageAgent.sendAudio(audioPath);
        }
    }

    @Override
    protected void sendImage(String s) {
        if (messageAgent != null) {
            messageAgent.sendImage(s);
        }
    }

    public void refreshMsgsFromDB() {
        new GetDataTask(mContext, false).execute();
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new GetDataTask(mContext, true).execute();
            }
        }, 1000);
    }

    @Override
    public void onLoadMore() {
    }

    private void initListView() {
        lvMessages.setPullRefreshEnable(true);
        lvMessages.setPullLoadEnable(false);
        lvMessages.setXListViewListener(this);
        lvMessages.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));
        lvMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Timber.v("onClick");
            }
        });

        lvMessages.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: 16/6/27 长按聊天消息显示『转发/复制』的 ContextMenu
                AVIMTypedMessage message = (AVIMTypedMessage) mMessageAdapter.getItem(position - 1);
                int type = mMessageAdapter.getItemViewType(position - 1);
                if (type == 0 || type == 1) {
                    AVIMTextMessage textMessage = (AVIMTextMessage) message;
                    mChatMessage = textMessage.getText();
                } else
                    mChatMessage = MessageHelper.getFilePath(message);

                return false;
            }
        });
    }

    private void setupPresenceGuardian() {
        scheduledExecutorServiceForPresence = Executors.newSingleThreadScheduledExecutor();

        long kTickForPresenceSending = 5;
        scheduledExecutorServiceForPresence.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        // call service
                        sendPresenceMessage();
                    }
                }, 0, kTickForPresenceSending, TimeUnit.SECONDS);
    }

    protected void bindAdapter(JSONObject object) {
        bindAdapterToListView(conversationType, object);

        refreshMsgsFromDB();
    }

    private void bindAdapterToListView(ConversationType conversationType, JSONObject object) {
        mMessageAdapter = new ChatMessageAdapter(this, conversationType, object);
        mMessageAdapter.setClickListener(new ChatMessageAdapter.ClickListener() {

            @Override
            public void onFailButtonClick(AVIMTypedMessage msg) {
                messageAgent.resendMsg(msg, defaultSendCallback);
            }

            @Override
            public void onLocationViewClick(AVIMLocationMessage locMsg) {

            }

            @Override
            public void onImageViewClick(AVIMImageMessage imageMsg) {
                ChatActivity chatActivity = (ChatActivity) mContext;
                ArrayList<String> paths = new ArrayList<>();
                paths.add(MessageHelper.getFilePath(imageMsg));
                chatActivity.showImageGallery(paths);
            }

            @Override
            public void onAudioLongClick(final AVIMAudioMessage audioMessage) {
                //弹出编辑文本(语音附加消息)，确认后上传。
                final EditText inputServer = new EditText(mContext);
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AlertDialogCustom);
                builder.setTitle("附加消息").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                        .setNegativeButton("取消", null);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //封装成MAP对象后转换为json
                        OkHttpUtils.get().url("https://leancloud.cn/1.1/rtm/messages/logs?convid=" + audioMessage.getConversationId())
                                .addHeader("X-LC-Id", "IwzlUusBdjf4bEGlypaqNRIx-gzGzoHsz")
                                .addHeader("X-LC-Key", "4iGQy4Mg1Q8o3AyvtUTGiFQl,master")
                                .build()
                                .execute(new StringCallback() {
                                    @Override
                                    public void onError(Call call, Exception e, int id) {

                                    }

                                    @Override
                                    public void onResponse(String response, int id) {
                                        Log.i("云端返回的JSON", response);
                                        com.alibaba.fastjson.JSONArray jsonArray = com.alibaba.fastjson.JSONArray.parseArray(response);
                                        for (int i = 0; i < jsonArray.size(); i++) {
                                            if (jsonArray.getJSONObject(i).get("msg-id").toString().equals(audioMessage.getMessageId())) {
                                                putJson2LeanChat(jsonArray.get(i).toString(), inputServer.getText().toString());
                                                return;
                                            }
                                        }
                                    }
                                });

                    }
                });
                builder.show();
            }

        });
        lvMessages.setAdapter(mMessageAdapter);
    }

    private void setupHeartBeatForPresenceCheckInSeconds() {
        scheduledExecutorServiceForPresenceCheckInSeconds = Executors.newSingleThreadScheduledExecutor();

        long kIntervalForLiveCheck = 3;
        scheduledExecutorServiceForPresenceCheckInSeconds.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        // call service
                        mHeartBeatTimesForRemainLive--;
                        if (mHeartBeatTimesForRemainLive < 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setTitleItemDrawable(R.drawable.offline);
                                }
                            });

                            scheduledExecutorServiceForPresenceCheckInSeconds.shutdown();
                        }
                    }
                }, 0, kIntervalForLiveCheck, TimeUnit.SECONDS);

    }

    private void putJson2LeanChat(String json, String text) {
        StringBuffer stringBuffer = new StringBuffer(json);
        stringBuffer.insert(stringBuffer.indexOf(":-3") + 3, ",\\\"_lctext\\\":" + "\\\"" + text + "\\\"");
        Log.i("new json ", stringBuffer.toString());

        Map<String, String> headers = new HashMap<>();
        headers.put("X-LC-Id", "IwzlUusBdjf4bEGlypaqNRIx-gzGzoHsz");
        headers.put("X-LC-Key", "4iGQy4Mg1Q8o3AyvtUTGiFQl,master");

        OkHttpUtils.put().url("https://leancloud.cn/1.1/rtm/messages/logs")
                .headers(headers)
                .requestBody(RequestBody.create(MediaType.parse("application/json"), stringBuffer.toString()))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Log.i("修改聊天记录 : ", response);
                    }
                });
    }

    private void requestAnonymousInfo() {
        String userId, agencyId;

        if (AuthHelper.getInstance().userAlreadyLogin() && AuthHelper.getInstance().iAmUser()) {
            userId = AuthHelper.getInstance().getUserId();
            agencyId = jsonFormatParams.optString("user_id");
        } else {
            agencyId = AuthHelper.getInstance().getUserId();
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
        monitorMessageAndUpdate();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (scheduledExecutorServiceForPresence != null) {
            scheduledExecutorServiceForPresence.shutdown();
        }

        if (scheduledExecutorServiceForPresenceCheckInSeconds != null) {
            scheduledExecutorServiceForPresenceCheckInSeconds.shutdown();
        }

        if (!TextUtils.isEmpty(cachedHouseIdForCurrentConv)) {
            prefs.edit().putString(Constants.kLastActivatedHouseId, cachedHouseIdForCurrentConv).apply();
        }
        if (!TextUtils.isEmpty(cachedHouseTradeTypeForCurrentConv)) {
            prefs.edit().putString(Constants.kLastActivatedHouseTradeType, cachedHouseTradeTypeForCurrentConv).apply();
        }
    }

    private void monitorMessageAndUpdate() {
        mMessageAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();

                if (mMessageAdapter.getDatas().size() == 0)
                    return;

                AVIMTypedMessage msg = mMessageAdapter.getDatas().get(mMessageAdapter.getDatas().size() - 1);

                JSONObject object = updateLastMessage(msg, true);

                // dispatch the event to WebViewBaseFragment
                EventBus.getDefault().post(new BridgeCallbackEvent(BridgeCallbackEvent.BridgeCallbackEventEnum.CALLBACK, object.toString()));
            }
        });
    }

    private void updateWebViewSettings() {
        WebView webView = mFragmentBBS.getWebView();

        if (webView == null) return;

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

        if (AuthHelper.getInstance().userAlreadyLogin() && AuthHelper.getInstance().iAmUser()) {
            url += AuthHelper.getInstance().getUserId() + "/" + jsonFormatParams.optString("user_id");
        } else {
            url += jsonFormatParams.optString("user_id") + "/" + AuthHelper.getInstance().getUserId();
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

                if (TextUtils.isEmpty(jsonFormatParams.optString("house_id"))) {
                    JSONObject object = constructFirstHouseInfo();
                    getWebViewFragment().getBridge().callHandler("getFirstHouseInfo", object.toString());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    // 定期发送在线状态
    private void sendPresenceMessage() {
        AVIMPresenceMessage msg = new AVIMPresenceMessage();
        msg.setOp(getString(R.string.txt_online));

        messageAgent.sendPresence(msg);
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

    public void onEvent(MessageEvent messageEvent) {
        AVIMTypedMessage msg = messageEvent.getMsg();
        if (msg.getConversationId().equals(conversation.getConversationId())) {
            if (messageEvent.getMsg().getClass().equals(AVIMPresenceMessage.class)) {
                // update presence status
                setTitleItemDrawable(R.drawable.online);

                int kTickForLiveCheck = 6;
                mHeartBeatTimesForRemainLive = kTickForLiveCheck;

                setupHeartBeatForPresenceCheckInSeconds();
            }

            roomsTable.clearUnread(conversation.getConversationId());
            refreshMsgsFromDB();
        }
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
                    ToastUtil.show(mContext, "暂不支持");
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

    private void initByIntent(Intent intent) {
        initData(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        initByIntent(intent);
    }

    @Override
    protected void onResume() {
        CacheService.setCurConv(conversation);

        super.onResume();

        if (conversation == null) {
            throw new IllegalStateException("conv is null");
        }
        setCurrentChattingConvid(conversation.getConversationId());
    }

    @Override
    protected void onDestroy() {
        CacheService.setCurConv(null);

        super.onDestroy();
    }

    public void initData(Intent intent) {
        String convid = intent.getStringExtra(CONVID);
        conversation = chatManager.lookUpConversationById(convid);
        if (conversation == null) {
            throw new NullPointerException("conv is null");
        }

        messageAgent = new MessageAgent(conversation);
        messageAgent.setSendCallback(defaultSendCallback);
        roomsTable.insertRoom(convid);
        roomsTable.clearUnread(conversation.getConversationId());
        conversationType = ConversationHelper.typeOfConv(conversation);
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
        if (data) {
            findViewById(R.id.fragmentBBS).setVisibility(View.VISIBLE);
        } else {
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

    private JSONObject constructFirstHouseInfo() {
        // 记录上一次的房源 id / trade_type，如果 web 没有传 house_id / trade_type 进聊天，则告诉 web 房源 id。
        // 如果没有上一次，就传可切换房源的第一条。
        JSONObject object = new JSONObject();

        String id = cachedHouseIdForCurrentConv, tradeType = cachedHouseTradeTypeForCurrentConv;

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(tradeType)) {
            id = prefs.getString(Constants.kLastActivatedHouseId, null);
            tradeType = prefs.getString(Constants.kLastActivatedHouseTradeType, null);

            if (TextUtils.isEmpty(id) && houseInfos != null && houseInfos.size() > 0) {
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

        return object;
    }

    @Override
    public void onGetFirstHouseInfo(String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
        if (null != callback) {
            callback.callback(constructFirstHouseInfo());
        }
    }

    private void resizeBBSBoard(int height) {
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fragmentBBS);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.height = height;

        frameLayout.setLayoutParams(layoutParams);
    }

    @Override
    protected void hideBottomLayoutAndScrollToLast() {
        scrollToLast();
    }

    public void scrollToLast() {
        lvMessages.post(new Runnable() {
            @Override
            public void run() {
                lvMessages.smoothScrollToPosition(lvMessages.getAdapter().getCount() - 1);
            }
        });
    }

    class GetDataTask extends NetAsyncTask {
        private List<AVIMTypedMessage> msgs;
        private boolean loadHistory;

        GetDataTask(Context ctx, boolean loadHistory) {
            super(ctx, false);
            this.loadHistory = loadHistory;
        }

        @Override
        protected void doInBack() throws Exception {
//            String msgId = null;
            long maxTime = System.currentTimeMillis() + 10 * 1000;
            int limit;
            long time;
            if (loadHistory == false) {
                time = maxTime;
                int count = mMessageAdapter.getCount();
                if (count > PAGE_SIZE) {
                    limit = count;
                } else {
                    limit = PAGE_SIZE;
                }
            } else {
                if (mMessageAdapter.getDatas().size() > 0) {
//                    msgId = mMessageAdapter.getDatas().get(0).getMessageId();
                    AVIMTypedMessage firstMsg = mMessageAdapter.getDatas().get(0);
                    time = firstMsg.getTimestamp();
                } else {
                    time = maxTime;
                }
                limit = PAGE_SIZE;
            }

            msgs = msgsTable.selectMsgs(conversation.getConversationId(), time, limit);

            cacheMsgs(msgs);
        }

        @Override
        protected void onPost(Exception e) {
            if (filterException(e)) {
                if (lvMessages.getPullRefreshing()) {
                    lvMessages.stopRefresh();
                }
                if (loadHistory == false) {
                    mMessageAdapter.setDatas(msgs);
                    mMessageAdapter.notifyDataSetChanged();
                    lvMessages.setSelection(ListView.FOCUS_DOWN);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollToLast();
                        }
                    },200);
                } else {
                    List<AVIMTypedMessage> newMsgs = new ArrayList<>();
                    newMsgs.addAll(msgs);
                    newMsgs.addAll(mMessageAdapter.getDatas());
                    mMessageAdapter.setDatas(newMsgs);
                    mMessageAdapter.notifyDataSetChanged();
                    if (msgs.size() > 0) {
                        lvMessages.setSelection(msgs.size() - 1);
                    } else {
                        ToastUtil.show(mContext, R.string.chat_activity_loadMessagesFinish);
                    }
                }
            }
        }

        void cacheMsgs(List<AVIMTypedMessage> msgs) throws Exception {
            Set<String> userIds = new HashSet<>();
            for (AVIMTypedMessage msg : msgs) {
                AVIMReservedMessageType type = AVIMReservedMessageType.getAVIMReservedMessageType(msg.getMessageType());
                if (type == AVIMReservedMessageType.AudioMessageType) {
                    File file = new File(MessageHelper.getFilePath(msg));
                    if (!file.exists()) {
                        AVIMAudioMessage audioMsg = (AVIMAudioMessage) msg;
                        String url = audioMsg.getFileUrl();
                        DownloadUtils.downloadFileIfNotExists(url, file);
                    }
                }
                userIds.add(msg.getFrom());
            }
            if (chatManager.getUserInfoFactory() == null) {
                throw new NullPointerException("chat user factory is null");
            }
            chatManager.getUserInfoFactory().cacheUserInfoByIdsInBackground(new ArrayList<>(userIds));
        }

    }

    class DefaultSendCallback implements MessageAgent.SendCallback {

        @Override
        public void onError(Exception e) {
            refreshMsgsFromDB();
        }

        @Override
        public void onSuccess(AVIMTypedMessage msg) {
            refreshMsgsFromDB();
        }
    }
}
