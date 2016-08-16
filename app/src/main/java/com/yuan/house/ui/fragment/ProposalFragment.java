package com.yuan.house.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.leanchatlib.adapter.ChatMessageAdapter;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.view.xlist.XListView;
import com.baoyz.actionsheet.ActionSheet;
import com.dimo.utils.StringUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.yuan.house.R;
import com.yuan.house.activities.WebViewBasedActivity;
import com.yuan.house.adapter.ProposalListAdapter;
import com.yuan.house.application.Injector;
import com.yuan.house.bean.BrokerInfo;
import com.yuan.house.bean.ProposalInfo;
import com.yuan.house.common.Constants;
import com.yuan.house.enumerate.ProposalMediaType;
import com.yuan.house.enumerate.ProposalMessageCategory;
import com.yuan.house.enumerate.ProposalSourceType;
import com.yuan.house.event.InputBottomBarEvent;
import com.yuan.house.event.InputBottomBarRecordEvent;
import com.yuan.house.event.InputBottomBarTextEvent;
import com.yuan.house.event.NotificationEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.ui.view.InputBottomBar;
import com.yuan.house.ui.view.PickerPopWindow;
import com.yuan.house.utils.DateUtil;
import com.yuan.house.utils.FileUtil;
import com.yuan.house.utils.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import okhttp3.Call;
import timber.log.Timber;

/**
 * 用户端和中介端合并在一个Fragment里
 * Created by KevinLee on 2016/4/24.
 */
public class ProposalFragment extends WebViewBaseFragment implements XListView.IXListViewListener {
    private static final String TAG = "ProposalFragment";
    public static ProposalSourceType sourceType = ProposalSourceType.UNKNOWN;
    public static ProposalMediaType msg_type = ProposalMediaType.TEXT;                            //1:文本，2：语音，3：图片
    public static ProposalMessageCategory category = ProposalMessageCategory.SUGGESTION;          //0：投诉；1：建议；2：BUG
    protected OnProposalInteractionListener mBridgeListener;
    protected ProposalListAdapter adapter;
    @BindView(R.id.proposal)
    Button proposal;
    @BindView(R.id.complaint)
    Button complaint;
    @BindView(R.id.bug)
    Button bug;
    @BindView(R.id.chat_inputbottombar)
    InputBottomBar inputBottomBar;
    @BindView(R.id.lvMessages)
    XListView xListView;
    @BindView(R.id.proposal_scrollView)
    ScrollView scrollView;

    private int currentPageNumOfProposal = 1;
    private int currentPageNumOfSuggestion = 1;
    private int currentPageNumOfBug = 1;

    private String content;
    private int duration = 0;       //录音时长

    private PickerPopWindow mBrokerPicker = null;
    private ArrayList mBrokerList = new ArrayList();

    private String currentAudioPath = null;

    public static ProposalFragment newInstance() {
        ProposalFragment fragment = new ProposalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_proposal, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.bind(this, view);

        if (AuthHelper.getInstance().iAmUser()) {
            redirectToLoadUrl(Constants.kWebPageUserCenter);
            sourceType = ProposalSourceType.FROM_USER;
        } else {
            redirectToLoadUrl(Constants.kWebPageAgencyCenter);
            sourceType = ProposalSourceType.FROM_AGENCY;
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        proposal.performClick();

        inputBottomBar.setShowDefaultActionLayout(false);

        if (AuthHelper.getInstance().userAlreadyLogin()) {
            getBrokers();

            initListView();
        }
    }

    private void showOptionSheet() {
        ActionSheet.createBuilder(getContext(), getActivity().getSupportFragmentManager())
                .setCancelButtonTitle(R.string.cancel)
                .setOtherButtonTitles("上传图片", "投诉经纪人")
                .setCancelableOnTouchOutside(true)
                .setListener(new ActionSheet.ActionSheetListener() {
                    @Override
                    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {
                        actionSheet.dismiss();
                    }

                    @Override
                    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
                        switch (index) {
                            case 0: {
                                msg_type = ProposalMediaType.IMAGE;
                                mBridgeListener.onSelectImageForProposal();
                            }
                            break;
                            case 1: {
                                if (mBrokerPicker == null) {
                                    if (mBrokerList.size() > 0) {
                                        initBrokerView();
                                    } else {
                                        ToastUtil.showShort(getContext(), "没有经纪人列表");
                                    }
                                } else {
                                    mBrokerPicker.showPopWin(getActivity());
                                }
                            }
                            break;
                        }

                        actionSheet.dismiss();
                    }
                }).show();
    }

    private void initListView() {
        xListView.setPullRefreshEnable(true);
        xListView.setPullLoadEnable(false);
        xListView.setXListViewListener(this);
        xListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));
        xListView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    scrollView.requestDisallowInterceptTouchEvent(false);
                } else {
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
        bindAdapterToListView();
        getHistoryMessages(getCurrentPageNum());
    }

    private void bindAdapterToListView() {
        adapter = new ProposalListAdapter(getActivity(), ConversationType.Single, new JSONObject());
        adapter.setCurrentDatas(category);
        adapter.setClickListener(new ChatMessageAdapter.ClickListener() {
            @Override
            public void onFailButtonClick(AVIMTypedMessage msg) {
                // messageAgent.resendMsg(msg, defaultSendCallback);
            }

            @Override
            public void onLocationViewClick(AVIMLocationMessage locMsg) {

            }

            @Override
            public void onImageViewClick(AVIMImageMessage imageMsg) {
                WebViewBasedActivity chatActivity = (WebViewBasedActivity) getActivity();
                ArrayList<String> paths = new ArrayList<>();
                paths.add(imageMsg.getText());
                chatActivity.showImageGallery(paths);
            }

            @Override
            public void onAudioLongClick(final AVIMAudioMessage audioMessage) {
            }

        });
        xListView.setAdapter(adapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFragmentListener = (OnFragmentInteractionListener) activity;
            mBridgeListener = (OnProposalInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
        mBridgeListener = null;
    }

    /**
     * 经纪人列表
     */
    private void initBrokerView() {
        // mBrokerList = new ArrayList();
        ArrayList item2 = new ArrayList();
        ArrayList item3 = new ArrayList();
        ArrayList selection = new ArrayList();

        // getBrokers();

        for (int i = 0; i < 3; i++) {
            //   mBrokerList.add(" ");
            selection.add("   ");
        }

        mBrokerPicker = new PickerPopWindow(getActivity(), mBrokerList, item2, item3, selection,
                new PickerPopWindow.OnPickCompletedListener() {
                    @Override
                    public void onAddressPickCompleted(String item1, String item2, String item3) {
                        if (!TextUtils.isEmpty(item1)) {
                            inputBottomBar.showTextLayout();
                            inputBottomBar.getEditTextView().setText(item1);
                            inputBottomBar.getEditTextView().setSelection(item1.length());
                        }
                    }
                });
    }

    @OnClick({R.id.proposal, R.id.complaint, R.id.bug})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.complaint: {
                complaint.setSelected(true);
                proposal.setSelected(false);
                bug.setSelected(false);
                category = ProposalMessageCategory.COMPLAINT;
                switchPage(category);
                break;
            }
            case R.id.proposal: {
                complaint.setSelected(false);
                proposal.setSelected(true);
                bug.setSelected(false);
                category = ProposalMessageCategory.SUGGESTION;
                switchPage(category);
                break;
            }
            case R.id.bug: {
                complaint.setSelected(false);
                proposal.setSelected(false);
                bug.setSelected(true);
                category = ProposalMessageCategory.BUG;
                switchPage(category);
                break;
            }
        }
    }

    private void switchPage(ProposalMessageCategory type) {
        if (adapter != null) {
            adapter.setCurrentDatas(type);
            adapter.notifyDataSetChanged();
            scrollToLast();

            if (getCurrentPageNum() == 1) {
                getHistoryMessages(1);
            }
        }
    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onLoadMore() {
        int page = currentPageNumIncrease();//mCurrentPage + 1;
        getHistoryMessages(page);
    }

    private int currentPageNumIncrease() {
        int currentNum = 1;
        switch (category) {
            case COMPLAINT:
                currentNum = ++currentPageNumOfProposal;
                break;
            case SUGGESTION:
                currentNum = ++currentPageNumOfSuggestion;
                break;
            case BUG:
                currentNum = ++currentPageNumOfBug;
                break;
        }
        return currentNum;
    }

    private int getCurrentPageNum() {
        switch (category) {
            case COMPLAINT:
                return currentPageNumOfProposal;
            case SUGGESTION:
                return currentPageNumOfSuggestion;
            case BUG:
                return currentPageNumOfBug;
            default:
                return 1;
        }
    }

    /**
     * 输入文字事件处理
     */
    public void onEvent(InputBottomBarTextEvent textEvent) {
        if (null != textEvent && !TextUtils.isEmpty(textEvent.sendContent)) {
            msg_type = ProposalMediaType.TEXT;
            content = textEvent.sendContent;
            sendMessage();
        }
    }

    /**
     * 输入语音事件处理
     */
    public void onEvent(InputBottomBarRecordEvent recordEvent) {
        if (null != recordEvent && !TextUtils.isEmpty(recordEvent.audioPath)) {
            msg_type = ProposalMediaType.AUDIO;
            duration = recordEvent.audioDuration;
            currentAudioPath = recordEvent.audioPath;
            uploadFile(recordEvent.audioPath);
        }
    }

    /**
     * 发送图片等事件处理
     */
    public void onEvent(InputBottomBarEvent event) {
        switch (event.eventAction) {
            case InputBottomBarEvent.INPUTBOTTOMBAR_ACTION:
                showOptionSheet();
                break;
        }
    }

    /**
     * 上次图片、声音等文件
     */
    public void uploadFile(String filePath) {
        OkHttpUtils.post().url(Constants.kWebServiceFileUpload)
                .addHeader(Constants.kHttpReqKeyContentType, "multipart/form-data")
                .addHeader(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken())
                //.file(new File(filePath))
                .addFile("file[]", filePath, new File(filePath))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Timber.e("uploadfile failed");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); ++i) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                if (!handleErrorCode(jsonObject)) {
                                    content = jsonObject.optString("original");
                                    content = content.substring(2, content.length() - 2);
                                    if (!TextUtils.isEmpty(content)) {
                                        sendMessage();
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            ToastUtil.showShort(getContext(), "提交失败");
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * 发送投诉/建议/BUG信息
     */
    private void sendMessage() {
        Map<String, Object> params = new HashMap<>();
        params.put("msg_type", msg_type.ordinal());
        params.put("type", sourceType.ordinal());
        params.put("content", content);
        params.put("complain_agency_id", 0);
        params.put("category", category.ordinal());
        params.put("duration", duration);

        OkHttpUtils.postString().url(Constants.kWebServiceSendFeedback)
                .addHeader(Constants.kHttpReqKeyContentType, "application/json")
                .addHeader(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken())
                .content(com.alibaba.fastjson.JSONObject.toJSONString(params))
                .mediaType(okhttp3.MediaType.parse("application/json"))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Timber.e("Feedback error");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (!handleErrorCode(jsonObject)) {
                                Timber.v("提交成功");

                                ProposalInfo data = parseProposalInfo(jsonObject);
                                if (data.msg_type == ProposalMediaType.AUDIO.ordinal()) {
                                    getAudioFile(data);
                                }
                                addData2Adapter(data, true);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                                scrollToLast();
                            }
                        } catch (JSONException e) {
                            ToastUtil.showShort(getContext(), "提交失败, 请检查网络连接。");
                            e.printStackTrace();
                        }
                    }
                });

    }

    private void scrollToLast() {
        /*if (lvMessages.getAdapter().getCount() <= 0) {
            return;
        }

        lvMessages.post(new Runnable() {
            @Override
            public void run() {
                lvMessages.smoothScrollToPosition(lvMessages.getAdapter().getCount() - 1);
            }
        });*/
    }

    private void addData2Adapter(ProposalInfo data, boolean order) {
        if (data.category != category.ordinal()) {
            return;
        }

        AVIMTypedMessage msg = null;
        if (data.msg_type == ProposalMediaType.TEXT.ordinal()) {
            AVIMTextMessage message = new AVIMTextMessage();
            message.setText(data.content);
            setAVIMessage(message, data);
            msg = message;
        } else if (data.msg_type == ProposalMediaType.IMAGE.ordinal()) {
            AVIMImageMessage imageMsg = new AVIMImageMessage();
            String url = data.content.replace("\\", "");
            imageMsg.setText(url);
            setAVIMessage(imageMsg, data);
            msg = imageMsg;
        } else if (data.msg_type == ProposalMediaType.AUDIO.ordinal()) {
            try {
                AVIMAudioMessage audioMessage = new AVIMAudioMessage(data.content);
                setAVIMessage(audioMessage, data);
                msg = audioMessage;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (msg != null && adapter != null) {
            if (order) {
                adapter.add(msg, category);
            } else {
                adapter.add2First(msg, category);
            }
        }
    }

    private void setAVIMessage(AVIMMessage message, ProposalInfo data) {
        ChatManager chatManager = ChatManager.getInstance();
        String selfId = chatManager.getSelfId();
        message.setFrom(selfId);
        String localTime = DateUtil.utc2Local(data.created_at, DateUtil.TIME_FORMAT, DateUtil.TIME_FORMAT);
        long timeStamp = DateUtil.convert2long(localTime, DateUtil.TIME_FORMAT);
        message.setTimestamp(timeStamp);
        message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusReceipt);
    }

    private boolean handleErrorCode(JSONObject jsonObject) {
        int errCode = jsonObject.optInt("error_code", 0);
        switch (errCode) {
            case 450:
            case 401: {
                // 重新登录
                EventBus.getDefault().post(new NotificationEvent(NotificationEvent.NotificationEventEnum.KICK_OUT, null));
            }
            break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 获取建议/投诉/BUG的历史纪录
     */
    private void getHistoryMessages(int page) {
        OkHttpUtils.get().url(Constants.kWebServiceGetFeedback + page)
                .addHeader(Constants.kHttpReqKeyContentType, "application/json")
                .addHeader(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken())
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onError(Call call, Exception e, int id) {
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); ++i) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                ProposalInfo data = parseProposalInfo(jsonObject);
                                if (data.msg_type == ProposalMediaType.AUDIO.ordinal()) {
                                    currentAudioPath = null;
                                    getAudioFile(data);
                                }
                                addData2Adapter(data, false);
                            }
                            adapter.notifyDataSetChanged();
                            scrollToLast();
                            currentPageNumIncrease(); //++mCurrentPage;

                        } catch (JSONException e) {
                            e.printStackTrace();
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                handleErrorCode(jsonObject);
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                        } finally {
//                            mSwipeRefreshWidget.setRefreshing(false);
                        }
                    }
                });
    }

    /**
     * 下载语音
     */
    private void getAudioFile(ProposalInfo data) {
        String audioUrl = data.content;

        if (!StringUtil.isValidHTTPUrl(audioUrl)) {
            Log.e(TAG, "getAudioFile url err!");
            return;
        }

        String fileName = audioUrl.substring(audioUrl.lastIndexOf("/") + 1);
        String path = FileUtil.getAudioFile();
        data.content = path + fileName;

        if (!TextUtils.isEmpty(currentAudioPath)) {
            // 刚发出语音,只需要拷贝本地语音文件
            FileUtil.copyFile(currentAudioPath, data.content);
            return;
        }

        if (!FileUtil.isFileExists(path + fileName)) {
            FileUtil.downloadFile(audioUrl, path, fileName);
        }
    }

    /**
     * 获取经纪人名单
     */
    private void getBrokers() {
        OkHttpUtils.get().url(Constants.kWebServiceGetBrokers)
                .addHeader(Constants.kHttpReqKeyContentType, "application/json")
                .addHeader(Constants.kHttpReqKeyAuthToken, AuthHelper.getInstance().getUserToken())
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                BrokerInfo info = parseBrokerInfo(jsonObject);
                                mBrokerList.add(info.user_id);
                            }
                        } catch (Exception e) {

                        }
                    }
                });
    }

    private ProposalInfo parseProposalInfo(JSONObject jsonObject) {
        ProposalInfo data = new ProposalInfo();
        data.user_id = jsonObject.optString("user_id", "");
        data.msg_type = jsonObject.optInt("msg_type", 0);
        data.content = jsonObject.optString("content", "");
        data.type = jsonObject.optInt("type", 0);
        data.complain_agency_id = jsonObject.optInt("complain_agency_id", 0);
        data.category = jsonObject.optInt("category", 0);
        data.duration = jsonObject.optInt("duration", 0);
        data.created_at = jsonObject.optString("created_at", "");
        data.updated_at = jsonObject.optString("updated_at", "");
        return data;
    }

    private BrokerInfo parseBrokerInfo(JSONObject jsonObject) {
        BrokerInfo brokerInfo = new BrokerInfo();
        brokerInfo.user_id = jsonObject.optString("user_id", "id");
        return brokerInfo;
    }

    public interface OnProposalInteractionListener extends WebViewBaseFragment.OnBridgeInteractionListener {
        void onSelectImageForProposal();
    }
}
