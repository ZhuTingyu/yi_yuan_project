package com.yuan.house.ui.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.avoscloud.leanchatlib.activity.ImageBrowserActivity;
import com.avoscloud.leanchatlib.adapter.ChatNewMessageAdapter;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.avoscloud.leanchatlib.controller.MessageHelper;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.utils.PathUtils;
import com.avoscloud.leanchatlib.utils.ProviderPathUtils;
import com.avoscloud.leanchatlib.view.xlist.XListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.yuan.house.R;
import com.yuan.house.adapter.ProposalAdapter;
import com.yuan.house.adapter.ProposalListAdapter;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.bean.ProposalInfo;
import com.yuan.house.common.Constants;
import com.yuan.house.enumerate.ProposalMediaType;
import com.yuan.house.enumerate.ProposalMessageCategory;
import com.yuan.house.enumerate.ProposalSourceType;
import com.yuan.house.event.InputBottomBarEvent;
import com.yuan.house.event.InputBottomBarRecordEvent;
import com.yuan.house.event.InputBottomBarTextEvent;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.ui.view.InputBottomBar;
import com.yuan.house.ui.view.PickerPopWindow;
import com.yuan.house.utils.FileUtil;
import com.yuan.house.utils.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
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
import okhttp3.Call;

/**
 * 用户端和中介端合并在一个Fragment里
 * Created by KevinLee on 2016/4/24.
 */
public class ProposalFragment extends WebViewBaseFragment implements XListView.IXListViewListener {

    private static final int TAKE_CAMERA_REQUEST = 2;
    private static final int GALLERY_REQUEST = 0;
    private static final int GALLERY_KITKAT_REQUEST = 3;
    protected String localCameraPath;

    protected OnProposalInteractionListener mBridgeListener;

    @BindView(R.id.proposal)
    Button proposal;

    @BindView(R.id.complaint)
    Button complaint;

    @BindView(R.id.bug)
    Button bug;

    @BindView(R.id.chat_inputbottombar)
    InputBottomBar inputBottomBar;

    @BindView(R.id.listview)
    XListView xListView;

    @BindView(R.id.proposal_scrollView)
    ScrollView scrollView;

    protected ProposalListAdapter adapter;
    private int mCurrentPage = 1;

    TextView app_upload_image, app_complaint, app_cancle;

    public static ProposalSourceType sourceType = ProposalSourceType.UNKNOWN;
    public static ProposalMediaType msg_type = ProposalMediaType.TEXT;                            //1:文本，2：语音，3：图片
    public static ProposalMessageCategory category = ProposalMessageCategory.SUGGESTION;          //0：投诉；1：建议；2：BUG
    private String content;
    private int duration = 0;       //录音时长


    private View mPopView;
    private PopupWindow mPopupWindow;
    private WindowManager.LayoutParams params;

    private PickerPopWindow mBrokerPicker;
    private ArrayList mBrokerList;


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

        if (DMApplication.getInstance().iAmUser()) {
            redirectToLoadUrl(Constants.kWebpageUserCenter);
            sourceType = ProposalSourceType.FROM_USER;
        } else {
            redirectToLoadUrl(Constants.kWebpageAgencyCenter);
            sourceType = ProposalSourceType.FROM_AGENCY;
        }

        localCameraPath = PathUtils.getPicturePathByCurrentTime(getContext());
        //   EventBus.getDefault().register(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        proposal.performClick();

        this.params = getActivity().getWindow().getAttributes();

        inputBottomBar.setShowDefaultActionLayout(false);
        initPopupView();
        initBrokerView();
        initListView();
    }


    private void initListView() {
        xListView.setPullRefreshEnable(true);
        xListView.setPullLoadEnable(false);
        xListView.setXListViewListener(this);
        xListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));
        xListView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    scrollView.requestDisallowInterceptTouchEvent(false);
                }else{
                    scrollView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
        bindAdapterToListView();
        getHistoryMessages(mCurrentPage);
    }

    private void bindAdapterToListView() {
        adapter = new ProposalListAdapter((Context) mBridgeListener, ConversationType.Single, new JSONObject());
        adapter.setCurrentDatas(category);
        adapter.setClickListener(new ChatNewMessageAdapter.ClickListener() {

            @Override
            public void onFailButtonClick(AVIMTypedMessage msg) {
               // messageAgent.resendMsg(msg, defaultSendCallback);
            }

            @Override
            public void onLocationViewClick(AVIMLocationMessage locMsg) {

            }

            @Override
            public void onImageViewClick(AVIMImageMessage imageMsg) {
                ImageBrowserActivity.go((Context) mBridgeListener,
                        MessageHelper.getFilePath(imageMsg),
                        imageMsg.getFileUrl());
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

    private void initPopupView() {
        mPopView = LayoutInflater.from(getContext()).inflate(R.layout.popup_other, null);
        app_cancle = (TextView) mPopView.findViewById(R.id.app_cancle);
        app_upload_image = (TextView) mPopView.findViewById(R.id.app_upload_image);
        app_complaint = (TextView) mPopView.findViewById(R.id.app_complaint);

        mPopupWindow = new PopupWindow(mPopView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        app_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopupWindow();
            }
        });

        app_upload_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopupWindow();
                msg_type = ProposalMediaType.IMAGE;
                mBridgeListener.onSelectImageForProposal();
            }
        });

        app_complaint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBrokerPicker.showPopWin(getActivity());
                closePopupWindow();
            }
        });
    }

    /**
     * 经纪人列表
     */
    private void initBrokerView() {

        mBrokerList = new ArrayList();
        ArrayList item2 = new ArrayList();
        ArrayList item3 = new ArrayList();
        ArrayList selection = new ArrayList();

        getBrokers();

        for (int i = 0; i < 10; i++) {
            mBrokerList.add("test" + i);
        }
        selection.add("");
        selection.add("");
        selection.add("");

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
                switchAdapter(category);
                break;
            }
            case R.id.proposal: {
                complaint.setSelected(false);
                proposal.setSelected(true);
                bug.setSelected(false);
                category = ProposalMessageCategory.SUGGESTION;
                switchAdapter(category);
                break;
            }
            case R.id.bug: {
                complaint.setSelected(false);
                proposal.setSelected(false);
                bug.setSelected(true);
                category = ProposalMessageCategory.BUG;
                switchAdapter(category);
                break;
            }
        }
    }

    private void switchAdapter(ProposalMessageCategory type) {
        if (adapter != null) {
            adapter.setCurrentDatas(type);
            adapter.notifyDataSetChanged();
        }
    }

    private void onBtnMore() {
        mPopupWindow.showAtLocation(mWebView, Gravity.BOTTOM, 0, 0);
        mPopupWindow.setAnimationStyle(R.style.app_pop);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.update();
        params.alpha = 0.7f;
        setWindowAttributes();
    }

    private void closePopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            params.alpha = 1;
            setWindowAttributes();
        }
    }

    private void setWindowAttributes() {
        getActivity().getWindow().setAttributes(params);
    }

    @Override
    public void onRefresh() {
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new GetDataTask(mContext, true).execute();
            }
        }, 1000);*/
    }

    @Override
    public void onLoadMore() {
        int page = mCurrentPage + 1;
        getHistoryMessages(page);
    }

    public interface OnProposalInteractionListener extends WebViewBaseFragment.OnBridgeInteractionListener {
        void onUploadProposalAudio(String data);

        void onSelectImageForProposal();
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
            uploadFile(recordEvent.audioPath);
        }
    }

    /**
     * 发送图片等事件处理
     */
    public void onEvent(InputBottomBarEvent event) {
        if (null != event) {
            switch (event.eventAction) {
                case InputBottomBarEvent.INPUTBOTTOMBAR_ACTION:
                    onBtnMore();
                    break;
                case InputBottomBarEvent.INPUTBOTTOMBAR_IMAGE_ACTION:
                    msg_type = ProposalMediaType.IMAGE;
                    selectImageFromLocal();
                    break;
                case InputBottomBarEvent.INPUTBOTTOMBAR_CAMERA_ACTION:
                    msg_type = ProposalMediaType.IMAGE;
                    selectImageFromCamera();
                    break;
            }
        }
    }

    /**
     * 上次图片等文件
     */
    public void uploadFile(String filePath) {

        OkHttpUtils.post().url(Constants.kWebServiceFileUpload)
                .addHeader("Content-Type", "multipart/form-data")
                .addHeader("token", AuthHelper.userToken())
                //.file(new File(filePath))
                .addFile("file[]", filePath, new File(/*Environment.getExternalStorageDirectory() + */filePath))
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onError(Call call, Exception e) {

                    }

                    @Override
                    public void onResponse(String response) {
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
                .addHeader("Content-Type", "application/json")
                .addHeader("token", AuthHelper.userToken())
                .content(com.alibaba.fastjson.JSONObject.toJSONString(params))
                .mediaType(okhttp3.MediaType.parse("application/json"))
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onError(Call call, Exception e) {

                    }

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (!handleErrorCode(jsonObject)) {
                                ToastUtil.showShort(getContext(), "提交成功");
                                ProposalInfo data = parse2MyData(jsonObject);
                                if (data.msg_type == ProposalMediaType.AUDIO.ordinal()) {
                                    getAudioFile(data);
                                }
                                addData2Adapter(data);
                                adapter.notifyDataSetChanged();
                                //myAdapter.addData(data);
                                //myAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            ToastUtil.showShort(getContext(), "提交失败");
                            e.printStackTrace();
                        }
                    }
                });

    }

    private void addData2Adapter(ProposalInfo data) {
        ChatManager chatManager = ChatManager.getInstance();
        String selfId = chatManager.getSelfId();

        if (data.msg_type == ProposalMediaType.TEXT.ordinal()) {
            AVIMTextMessage message = new AVIMTextMessage();
            message.setText(data.content);
            message.setFrom(selfId);
            message.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusReceipt);
            adapter.add(message, category);
        }
        else if (data.msg_type == ProposalMediaType.IMAGE.ordinal()) {
            AVIMImageMessage imageMsg = new AVIMImageMessage();
            imageMsg.setText(data.content);
            imageMsg.setFrom(selfId);
            imageMsg.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusReceipt);
            adapter.add(imageMsg, category);
        }
        else if (data.msg_type == ProposalMediaType.AUDIO.ordinal()) {
            try {
                AVIMAudioMessage audioMessage = new AVIMAudioMessage(data.content);
                audioMessage.setFrom(selfId);
                audioMessage.setMessageStatus(AVIMMessage.AVIMMessageStatus.AVIMMessageStatusReceipt);
                adapter.add(audioMessage, category);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean handleErrorCode(JSONObject jsonObject) {
        int errCode = jsonObject.optInt("error_code", 0);
        switch (errCode) {
            case 450:
            case 401:{
                ToastUtil.showShort(getContext(), jsonObject.optString("error_msg", "未知错误!"));
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
                .addHeader("Content-Type", "application/json")
                .addHeader("token", AuthHelper.userToken())
                .build()
                .execute(new StringCallback() {

                    @Override
                    public void onError(Call call, Exception e) {
                    }

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); ++i) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                ProposalInfo data = parse2MyData(jsonObject);
                                if (data.msg_type == ProposalMediaType.AUDIO.ordinal()) {
                                    getAudioFile(data);
                                }
                                addData2Adapter(data);
                                //myAdapter.addData(data);
                            }
                            adapter.notifyDataSetChanged();
                            //myAdapter.notifyDataSetChanged();
                            ++mCurrentPage;

                        } catch (JSONException e) {
                            e.printStackTrace();
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
        String fileName = audioUrl.substring(audioUrl.lastIndexOf("/")+1);
        String path = FileUtil.getAudioFile();
        data.content = path + fileName;

        if (!FileUtil.isFileExists(path + fileName)) {
            FileUtil.downloadFile(audioUrl, path, fileName);
        }
    }

    /**
     * 获取经纪人名单
     */
    private void getBrokers() {
        OkHttpUtils.get().url(Constants.kWebServiceGetBrokers)
                .addHeader("Content-Type", "application/json")
                .addHeader("token", AuthHelper.userToken())
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                    }

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            for (int i = 0; i < jsonArray.length(); i++) {

                            }
                        } catch (Exception e) {

                        }
                    }
                });
    }

    private ProposalInfo parse2MyData(JSONObject jsonObject) {
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
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageUri = Uri.fromFile(new File(localCameraPath));
        takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, TAKE_CAMERA_REQUEST);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST:
                case GALLERY_KITKAT_REQUEST:
                    if (data == null) {
                        //toast("return intent is null");
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
                        //                 getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    }
                    String localSelectPath = ProviderPathUtils.getPath(getActivity(), uri);
                    inputBottomBar.hideMoreLayout();
                    uploadFile(localSelectPath);
                    break;
                case TAKE_CAMERA_REQUEST:
                    inputBottomBar.hideMoreLayout();
                    uploadFile(localCameraPath);
                    break;
            }
        }
    }


}
