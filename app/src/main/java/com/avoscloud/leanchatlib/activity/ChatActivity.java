package com.avoscloud.leanchatlib.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;

import com.alibaba.fastjson.JSONArray;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMReservedMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avoscloud.leanchatlib.adapter.ChatEmotionGridAdapter;
import com.avoscloud.leanchatlib.adapter.ChatEmotionPagerAdapter;
import com.avoscloud.leanchatlib.adapter.ChatMessageAdapter;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.ConversationHelper;
import com.avoscloud.leanchatlib.controller.EmotionHelper;
import com.avoscloud.leanchatlib.controller.MessageAgent;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.db.MsgsTable;
import com.avoscloud.leanchatlib.db.RoomsTable;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.model.MessageEvent;
import com.avoscloud.leanchatlib.utils.DownloadUtils;
import com.avoscloud.leanchatlib.utils.NetAsyncTask;
import com.avoscloud.leanchatlib.utils.PathUtils;
import com.avoscloud.leanchatlib.utils.ProviderPathUtils;
import com.avoscloud.leanchatlib.view.EmotionEditText;
import com.avoscloud.leanchatlib.view.RecordButton;
import com.avoscloud.leanchatlib.view.xlist.XListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.yuan.house.R;
import com.yuan.house.activities.WebViewBasedActivity;
import com.yuan.house.utils.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ChatActivity extends WebViewBasedActivity implements OnClickListener,
        XListView.IXListViewListener {
    public static final String CONVID = "convid";
    private static final int PAGE_SIZE = 20;
    private static final int TAKE_CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 0;
    private static final int GALLERY_KITKAT_REQUEST = 3;

    private static ChatActivity chatInstance;
    //用来判断是否弹出通知
    private static String currentChattingConvid;
    protected ConversationType conversationType;
    protected AVIMConversation conversation;
    protected MsgsTable msgsTable;
    protected MessageAgent messageAgent;
    protected MessageAgent.SendCallback defaultSendCallback = new DefaultSendCallback();
    protected EventBus eventBus;
    protected ChatManager chatManager = ChatManager.getInstance();
    protected ChatMessageAdapter adapter;
    protected RoomsTable roomsTable;
    protected View chatTextLayout, chatAudioLayout, chatAddLayout, chatEmotionLayout;
    protected View sendBtn, addImageBtn, showAddBtn, addFileBtn, showEmotionBtn, addChangeHouseBtn;
    protected ImageButton btnModeSwitch;
    protected ViewPager emotionPager;
    protected EmotionEditText contentEdit;
    protected XListView xListView;
    protected RecordButton recordBtn;
    protected String localCameraPath = PathUtils.getTmpPath();
    protected View addCameraBtn;
    private LocationHandler locationHandler;
    private boolean mVoiceMode = false;

    public static ChatActivity getChatInstance() {
        return chatInstance;
    }

    public static String getCurrentChattingConvid() {
        return currentChattingConvid;
    }

    public static void setCurrentChattingConvid(String currentChattingConvid) {
        ChatActivity.currentChattingConvid = currentChattingConvid;
    }

    public void setLocationHandler(LocationHandler locationHandler) {
        this.locationHandler = locationHandler;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_layout, true, false);

        commonInit();

        initTitleView();

        findView();

        initEmotionPager();
        initRecordBtn();

        initListView();
        setSoftInputMode();
        initByIntent(getIntent());
    }

    private void initTitleView() {
        // FIXME: 16/6/11 conversation title
        setLeftItem(R.drawable.back, new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void findView() {
        xListView = (XListView) findViewById(R.id.listview);
        addImageBtn = findViewById(R.id.btnImageFromGallery);

        contentEdit = (EmotionEditText) findViewById(R.id.editChatField);
        chatTextLayout = findViewById(R.id.rl_field_textmode);
        btnModeSwitch = (ImageButton) findViewById(R.id.btnModeSwitch);
//        turnToTextBtn = findViewById(R.id.turnToTextBtn);
        recordBtn = (RecordButton) findViewById(R.id.recordBtn);
        chatAddLayout = findViewById(R.id.chatAddLayout);
        addFileBtn = findViewById(R.id.btnChooseFile);
        chatEmotionLayout = findViewById(R.id.chatEmotionLayout);
        showAddBtn = findViewById(R.id.btnMoreInput);
        showEmotionBtn = findViewById(R.id.btnEmotionInput);
//        sendBtn = findViewById(R.id.sendBtn);
        emotionPager = (ViewPager) findViewById(R.id.emotionPager);
        addCameraBtn = findViewById(R.id.btnImageFromCamera);
        addChangeHouseBtn = findViewById(R.id.btnSwitchHouse);

//        sendBtn.setOnClickListener(this);
        contentEdit.setOnClickListener(this);
        addImageBtn.setOnClickListener(this);
        addFileBtn.setOnClickListener(this);
        btnModeSwitch.setOnClickListener(this);
//        turnToTextBtn.setOnClickListener(this);
        showAddBtn.setOnClickListener(this);
        showEmotionBtn.setOnClickListener(this);
        addCameraBtn.setOnClickListener(this);
        addChangeHouseBtn.setOnClickListener(this);

//        addLocationBtn.setVisibility(View.GONE);
    }

    private void initByIntent(Intent intent) {
        initData(intent);
        refreshMsgsFromDB();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initByIntent(intent);
    }

    private void initListView() {
        xListView.setPullRefreshEnable(true);
        xListView.setPullLoadEnable(false);
        xListView.setXListViewListener(this);
        xListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));
    }

    private void initEmotionPager() {
        List<View> views = new ArrayList<View>();
        for (int i = 0; i < EmotionHelper.emojiGroups.size(); i++) {
            views.add(getEmotionGridView(i));
        }
        ChatEmotionPagerAdapter pagerAdapter = new ChatEmotionPagerAdapter(views);
        emotionPager.setOffscreenPageLimit(3);
        emotionPager.setAdapter(pagerAdapter);
    }

    private View getEmotionGridView(int pos) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View emotionView = inflater.inflate(R.layout.chat_emotion_gridview, null, false);
        GridView gridView = (GridView) emotionView.findViewById(R.id.gridview);
        final ChatEmotionGridAdapter chatEmotionGridAdapter = new ChatEmotionGridAdapter(mContext);
        List<String> pageEmotions = EmotionHelper.emojiGroups.get(pos);
        chatEmotionGridAdapter.setDatas(pageEmotions);
        gridView.setAdapter(chatEmotionGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String emotionText = (String) parent.getAdapter().getItem(position);
                int start = contentEdit.getSelectionStart();
                StringBuffer sb = new StringBuffer(contentEdit.getText());
                sb.replace(contentEdit.getSelectionStart(), contentEdit.getSelectionEnd(), emotionText);
                contentEdit.setText(sb.toString());

                CharSequence info = contentEdit.getText();
                if (info instanceof Spannable) {
                    Spannable spannable = (Spannable) info;
                    Selection.setSelection(spannable, start + emotionText.length());
                }
            }
        });
        return gridView;
    }

    public void initRecordBtn() {
        recordBtn.setSavePath(com.avoscloud.leanchatlib.utils.PathUtils.getRecordTmpPath());
        recordBtn.setRecordEventListener(new RecordButton.RecordEventListener() {
            @Override
            public void onFinishedRecord(final String audioPath, int secs) {
                messageAgent.sendAudio(audioPath);
            }

            @Override
            public void onStartRecord() {

            }
        });
    }

    private void switchInputMode() {
        mVoiceMode = !mVoiceMode;

        if (mVoiceMode) {
            btnModeSwitch.setImageResource(R.drawable.chat_btn_keyboard);

            ButterKnife.findById(this, R.id.rl_field_voicemode).setVisibility(View.VISIBLE);
            ButterKnife.findById(this, R.id.rl_field_textmode).setVisibility(View.GONE);
            hideSoftInputView();
        } else {
            btnModeSwitch.setImageResource(R.drawable.chat_btn_voice_selector);

            ButterKnife.findById(this, R.id.rl_field_voicemode).setVisibility(View.GONE);
            ButterKnife.findById(this, R.id.rl_field_textmode).setVisibility(View.VISIBLE);
        }
    }

    void commonInit() {
        mContext = this;
        chatInstance = this;
        msgsTable = MsgsTable.getCurrentUserInstance();
        roomsTable = RoomsTable.getCurrentUserInstance();
        eventBus = EventBus.getDefault();
        eventBus.register(this);
    }

    public void initData(Intent intent) {
        String convid = intent.getStringExtra(CONVID);
        conversation = chatManager.lookUpConversationById(convid);
        if (conversation == null) {
            throw new NullPointerException("conv is null");
        }
//        initActionBar(ConversationHelper.titleOfConv(conversation));

        messageAgent = new MessageAgent(conversation);
        messageAgent.setSendCallback(defaultSendCallback);
        roomsTable.insertRoom(convid);
        roomsTable.clearUnread(conversation.getConversationId());
        conversationType = ConversationHelper.typeOfConv(conversation);

        bindAdapterToListView(conversationType);
    }

    private void bindAdapterToListView(ConversationType conversationType) {
        adapter = new ChatMessageAdapter(this, conversationType);
        adapter.setClickListener(new ChatMessageAdapter.ClickListener() {
            @Override
            public void onFailButtonClick(AVIMTypedMessage msg) {
                messageAgent.resendMsg(msg, defaultSendCallback);
            }

            @Override
            public void onLocationViewClick(AVIMLocationMessage locMsg) {
                if (locationHandler != null) {
                    locationHandler.onLocationMessageViewClicked(ChatActivity.this, locMsg);
                }
            }

            @Override
            public void onImageViewClick(AVIMImageMessage imageMsg) {
                ImageBrowserActivity.go(ChatActivity.this,
                        MessageHelper.getFilePath(imageMsg),
                        imageMsg.getFileUrl());
            }

            @Override
            public void onAudioLongClick(final AVIMAudioMessage audioMessage) {
                //弹出编辑文本(语音附加消息)，确认后上传。
                final EditText inputServer = new EditText(ChatActivity.this);
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this, R.style.AlertDialogCustom);
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
                                    public void onError(Call call, Exception e) {

                                    }

                                    @Override
                                    public void onResponse(String response) {
                                        Log.i("云端返回的JSON", response);
                                        JSONArray jsonArray = JSONArray.parseArray(response);
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
        xListView.setAdapter(adapter);
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
                    public void onError(Call call, Exception e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.i("修改聊天记录 : ", response);
                    }
                });
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

    @Override
    public void onClick(View v) {
//        if (v.getId() == R.id.sendBtn) {
//            sendText();
//        } else
        if (v.getId() == R.id.btnImageFromGallery) {
            selectImageFromLocal();
        } else if (v.getId() == R.id.btnModeSwitch) {
            switchInputMode();
        } else if (v.getId() == R.id.btnMoreInput) {
            toggleBottomAddLayout();
        } else if (v.getId() == R.id.btnEmotionInput) {
            toggleEmotionLayout();
        } else if (v.getId() == R.id.btnChooseFile) {
            //文件
        } else if (v.getId() == R.id.editChatField) {
            hideBottomLayoutAndScrollToLast();
        } else if (v.getId() == R.id.btnImageFromCamera) {
            selectImageFromCamera();
        } else if (v.getId() == R.id.btnSwitchHouse) {
            // 显示推荐房源
            showSuggestedHouses();
        }
    }

    protected void showSuggestedHouses() {
        throw new NotImplementedException("IMPLEMENT IN DERIEVED CLASS");
    }

    protected void sendText() {
        throw new NotImplementedException("IMPLEMENT IN DERIEVED CLASS");
    }

    private void hideBottomLayoutAndScrollToLast() {
        hideBottomLayout();
        scrollToLast();
    }

    protected void hideBottomLayout() {
        hideAddLayout();
        chatEmotionLayout.setVisibility(View.GONE);
    }

    private void toggleEmotionLayout() {
        if (chatEmotionLayout.getVisibility() == View.VISIBLE) {
            chatEmotionLayout.setVisibility(View.GONE);
        } else {
            chatEmotionLayout.setVisibility(View.VISIBLE);
            hideAddLayout();
            hideSoftInputView();
        }
    }

    private void toggleBottomAddLayout() {
        if (chatAddLayout.getVisibility() == View.VISIBLE) {
            hideAddLayout();
        } else {
            chatEmotionLayout.setVisibility(View.GONE);
            hideSoftInputView();
            showAddLayout();
        }
    }

    private void hideAddLayout() {
        chatAddLayout.setVisibility(View.GONE);
    }

    private void showAddLayout() {
        chatAddLayout.setVisibility(View.VISIBLE);
    }

    public void selectImageFromLocal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.chat_activity_select_picture)),
                    GALLERY_REQUEST);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_KITKAT_REQUEST);
        }
    }

    public void selectImageFromCamera() {
        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageUri = Uri.fromFile(new File(localCameraPath));
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(openCameraIntent,
                TAKE_CAMERA_REQUEST);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST:
                case GALLERY_KITKAT_REQUEST:
                    if (data == null) {
                        ToastUtil.show(mContext, "return data is null");

                        return;
                    }
                    Uri uri;
                    if (requestCode == GALLERY_REQUEST) {
                        uri = data.getData();
                    } else {
                        //for Android 4.4
                        uri = data.getData();
                        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    }
                    String localSelectPath = ProviderPathUtils.getPath(mContext, uri);
                    messageAgent.sendImage(localSelectPath);
                    hideBottomLayout();
                    break;
                case TAKE_CAMERA_REQUEST:
                    messageAgent.sendImage(localCameraPath);
                    hideBottomLayout();
                    break;
            }
        }
    }

    public void scrollToLast() {
        xListView.post(new Runnable() {
            @Override
            public void run() {
                //fast scroll
        /*xListView.requestFocusFromTouch();
        xListView.setSelection(xListView.getAdapter().getCount() - 1);
        contentEdit.requestFocusFromTouch();*/
                xListView.smoothScrollToPosition(xListView.getAdapter().getCount() - 1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        chatInstance = null;
        eventBus.unregister(this);
        super.onDestroy();
    }

    public void onEvent(MessageEvent messageEvent) {
        AVIMTypedMessage msg = messageEvent.getMsg();
        if (msg.getConversationId().equals(conversation.getConversationId())) {
            roomsTable.clearUnread(conversation.getConversationId());
            refreshMsgsFromDB();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversation == null) {
            throw new IllegalStateException("conv is null");
        }
        setCurrentChattingConvid(conversation.getConversationId());
        chatManager.cancelNotification();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setCurrentChattingConvid(null);
    }

    void cacheMsgs(List<AVIMTypedMessage> msgs) throws Exception {
        Set<String> userIds = new HashSet<String>();
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
        chatManager.getUserInfoFactory().cacheUserInfoByIdsInBackground(new ArrayList<String>(userIds));
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
            String msgId = null;
            long maxTime = System.currentTimeMillis() + 10 * 1000;
            int limit;
            long time;
            if (loadHistory == false) {
                time = maxTime;
                int count = adapter.getCount();
                if (count > PAGE_SIZE) {
                    limit = count;
                } else {
                    limit = PAGE_SIZE;
                }
            } else {
                if (adapter.getDatas().size() > 0) {
                    msgId = adapter.getDatas().get(0).getMessageId();
                    AVIMTypedMessage firstMsg = adapter.getDatas().get(0);
                    time = firstMsg.getTimestamp();
                } else {
                    time = maxTime;
                }
                limit = PAGE_SIZE;
            }
            msgs = msgsTable.selectMsgs(conversation.getConversationId(), time, limit);
            //msgs = ConvManager.getInstance().queryHistoryMessage(conv, msgId, time, limit);
            cacheMsgs(msgs);
        }

        @Override
        protected void onPost(Exception e) {
            if (filterException(e)) {
                if (xListView.getPullRefreshing()) {
                    xListView.stopRefresh();
                }
                if (loadHistory == false) {
                    adapter.setDatas(msgs);
                    adapter.notifyDataSetChanged();
                    scrollToLast();
                } else {
                    List<AVIMTypedMessage> newMsgs = new ArrayList<AVIMTypedMessage>();
                    newMsgs.addAll(msgs);
                    newMsgs.addAll(adapter.getDatas());
                    adapter.setDatas(newMsgs);
                    adapter.notifyDataSetChanged();
                    if (msgs.size() > 0) {
                        xListView.setSelection(msgs.size() - 1);
                    } else {
                        ToastUtil.show(mContext, R.string.chat_activity_loadMessagesFinish);
                    }
                }
            }
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
