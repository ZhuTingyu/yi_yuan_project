package com.avoscloud.leanchatlib.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMReservedMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avoscloud.leanchatlib.adapter.ChatEmotionGridAdapter;
import com.avoscloud.leanchatlib.adapter.ChatEmotionPagerAdapter;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.EmotionHelper;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.db.MsgsTable;
import com.avoscloud.leanchatlib.db.RoomsTable;
import com.avoscloud.leanchatlib.utils.DownloadUtils;
import com.avoscloud.leanchatlib.utils.PathUtils;
import com.avoscloud.leanchatlib.view.EmotionEditText;
import com.avoscloud.leanchatlib.view.RecordButton;
import com.avoscloud.leanchatlib.view.xlist.XListView;
import com.yuan.house.R;
import com.yuan.house.activities.WebViewBasedActivity;

import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import me.nereo.multi_image_selector.MultiImageSelector;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;

public abstract class ChatActivity extends WebViewBasedActivity implements OnClickListener {
    public static final String CONVID = "convid";

    //用来判断是否弹出通知
    protected MsgsTable msgsTable;
    protected ChatManager chatManager = ChatManager.getInstance();
    protected RoomsTable roomsTable;
    protected View chatTextLayout, chatAddLayout;
    protected LinearLayout chatEmotionLayout;
    protected View showEmotionBtn;
    protected ImageButton btnModeSwitch, showAddBtn;
    protected TextView sendMsgBtn;
    protected ViewPager emotionPager;
    protected EmotionEditText contentEdit;
    protected XListView lvMessages;
    protected ListView lvGroups;
    protected RecordButton recordBtn;
    private boolean mVoiceMode = false;
    private View assistLayout;
    private int kActivityRequestCodeImagePickAndSend = 10;

    private boolean mHideText = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_layout, true, false);

        commonInit();

        initTitleView();

        findView();

        initEmotionPager();
        initRecordBtn();

        setSoftInputMode();

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
    }

    @Override
    public void onBackPressed() {
        if (assistLayout.getVisibility() == View.VISIBLE) {
            assistLayout.setVisibility(View.GONE);
        } else {
            finish();
        }
    }

    private void initTitleView() {
        // FIXME: 16/6/11 conversation title
        setLeftItem(R.drawable.btn_back, new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void findView() {
        lvMessages = (XListView) findViewById(R.id.lvMessages);
        lvGroups = (ListView) findViewById(R.id.lvGroups);

        contentEdit = (EmotionEditText) findViewById(R.id.editChatField);
        chatTextLayout = findViewById(R.id.rl_field_textmode);
        btnModeSwitch = (ImageButton) findViewById(R.id.btnModeSwitch);
        recordBtn = (RecordButton) findViewById(R.id.recordBtn);
        chatAddLayout = findViewById(R.id.chatAddLayout);
        chatEmotionLayout = (LinearLayout) findViewById(R.id.chatEmotionLayout);
        showAddBtn = (ImageButton) findViewById(R.id.btnMoreInput);
        sendMsgBtn = (TextView) findViewById(R.id.btnMoreSend);
        showEmotionBtn = findViewById(R.id.btnEmotionInput);
        emotionPager = (ViewPager) findViewById(R.id.emotionPager);
        assistLayout = findViewById(R.id.chatMoreLayout);

        contentEdit.setOnClickListener(this);
        btnModeSwitch.setOnClickListener(this);
        showAddBtn.setOnClickListener(this);
        sendMsgBtn.setOnClickListener(this);
        showEmotionBtn.setOnClickListener(this);

        contentEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String content = s.toString();
                if (TextUtils.isEmpty(content)) {
                    sendMsgBtn.setVisibility(View.GONE);
                    showAddBtn.setVisibility(View.VISIBLE);
                } else {
                    showAddBtn.setVisibility(View.GONE);
                    sendMsgBtn.setVisibility(View.VISIBLE);
                    if (chatAddLayout.getVisibility() == View.VISIBLE) {
                        chatAddLayout.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void initEmotionPager() {
        List<View> views = new ArrayList<>();
        for (int i = 0; i < EmotionHelper.emojiGroups.size(); i++) {
            views.add(getEmotionGridView(i));
        }
        ChatEmotionPagerAdapter pagerAdapter = new ChatEmotionPagerAdapter(views);
        emotionPager.setOffscreenPageLimit(3);
        emotionPager.setAdapter(pagerAdapter);
    }

    private View getEmotionGridView(int pos) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View emotionView = inflater.inflate(R.layout.chat_emotion_gridview, chatEmotionLayout, false);
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
        recordBtn.setSavePath(PathUtils.getRecordTmpPath());
        recordBtn.setRecordEventListener(new RecordButton.RecordEventListener() {
            @Override
            public void onFinishedRecord(final String audioPath, int secs) {
                sendAudio(audioPath);
            }

            @Override
            public void onStartRecord() {

            }
        });
    }

    private void switchInputMode() {
        mVoiceMode = !mVoiceMode;

        if (mVoiceMode) {
            if (sendMsgBtn.getVisibility() == View.VISIBLE) {
                sendMsgBtn.setVisibility(View.GONE);
                showAddBtn.setVisibility(View.VISIBLE);
                mHideText = true;
            }
            btnModeSwitch.setBackgroundResource(R.drawable.btn_keybord_switchover);
            ButterKnife.findById(this, R.id.rl_field_voicemode).setVisibility(View.VISIBLE);
            ButterKnife.findById(this, R.id.rl_field_textmode).setVisibility(View.GONE);
            hideSoftInputView();
            hideBottomLayout();
        } else {
            if (mHideText) {
                mHideText = false;
                sendMsgBtn.setVisibility(View.VISIBLE);
                showAddBtn.setVisibility(View.GONE);
            }
            btnModeSwitch.setBackgroundResource(R.drawable.chat_btn_voice_selector);

            ButterKnife.findById(this, R.id.rl_field_voicemode).setVisibility(View.GONE);
            ButterKnife.findById(this, R.id.rl_field_textmode).setVisibility(View.VISIBLE);
        }
    }

    void commonInit() {
        mContext = this;

        msgsTable = MsgsTable.getCurrentUserInstance();
        roomsTable = RoomsTable.getCurrentUserInstance();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onClick(View v) {
        //+++20160704 edward: use -1 instead of the unused id;
        if (v.getId() == -1) {
            //R.id.btnImageFromGallery
            selectImage();
        } else if (v.getId() == R.id.btnModeSwitch) {
            switchInputMode();
        } else if (v.getId() == R.id.btnMoreInput) {
            toggleBottomAddLayout();
        } else if (v.getId() == R.id.btnEmotionInput) {
            toggleEmotionLayout();
        } else if (v.getId() == R.id.editChatField) {
            hideBottomLayoutAndScrollToLast();
        } else if (v.getId() == -1) {
            // 显示推荐房源
            //R.id.btnSwitchHouse
            showSuggestedHouses();
        } else if (v.getId() == R.id.btnMoreSend) {
            sendText();
        }
    }

    protected void showSuggestedHouses() {
        throw new NotImplementedException("IMPLEMENT IN DERIEVED CLASS");
    }

    protected void sendText() {
        toggleBottomAddLayout();
    }

    protected void sendAudio(String audioPath) {
        throw new NotImplementedException("IMPLEMENT IN DERIEVED CLASS");
    }

    protected void sendImage(String s) {
        throw new NotImplementedException("IMPLEMENT IN DERIEVED CLASS");
    }

    protected void hideBottomLayoutAndScrollToLast() {
        hideBottomLayout();
    }

    protected void hideBottomLayout() {
        hideAddLayout();
        chatEmotionLayout.setVisibility(View.GONE);
        assistLayout.setVisibility(View.GONE);
    }

    private void showBottomLayout() {
        assistLayout.setVisibility(View.VISIBLE);
    }

    private void toggleEmotionLayout() {
        if (assistLayout.getVisibility() == View.VISIBLE) {
            chatEmotionLayout.setVisibility(View.GONE);
            hideBottomLayout();
        } else {
            showBottomLayout();
            chatEmotionLayout.setVisibility(View.VISIBLE);
            hideAddLayout();
            hideSoftInputView();
        }
    }


    private void toggleBottomAddLayout() {
        if (assistLayout.getVisibility() == View.VISIBLE) {
            hideAddLayout();
            hideBottomLayout();
        } else {
            mVoiceMode = false;
            btnModeSwitch.setBackgroundResource(R.drawable.chat_btn_voice_selector);
            ButterKnife.findById(this, R.id.rl_field_voicemode).setVisibility(View.GONE);
            ButterKnife.findById(this, R.id.rl_field_textmode).setVisibility(View.VISIBLE);

            chatEmotionLayout.setVisibility(View.GONE);
            hideSoftInputView();
            showBottomLayout();
            showAddLayout();
        }
    }

    private void hideAddLayout() {
        chatAddLayout.setVisibility(View.GONE);
    }

    private void showAddLayout() {
        chatAddLayout.setVisibility(View.VISIBLE);
    }

    public void selectImage() {
        int requestCode = kActivityRequestCodeImagePickAndSend;
        MultiImageSelector.create(mContext)
                .showCamera(true) // show camera or not. true by default
                .single()
                .start(this, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == kActivityRequestCodeImagePickAndSend && resultCode == RESULT_OK) {
            List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            sendImage(path.get(0));

            hideBottomLayout();
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        chatManager.cancelNotification();
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
