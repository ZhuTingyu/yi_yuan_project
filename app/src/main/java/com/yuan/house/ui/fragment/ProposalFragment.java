package com.yuan.house.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.enumerate.ProposalMediaType;
import com.yuan.house.enumerate.ProposalMessageCategory;
import com.yuan.house.enumerate.ProposalSourceType;
import com.yuan.house.ui.view.AudioRecorderButton;
import com.yuan.skeleton.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * 用户端和中介端合并在一个Fragment里
 * Created by KevinLee on 2016/4/24.
 */
public class ProposalFragment extends WebViewBaseFragment {
    private static final int SUCCESS = 1;
    private static final int TIMEOUT = 0;
    private final OkHttpClient client = new OkHttpClient();
    private final int SYS_INTENT_REQUEST = 0XFF01;
    protected OnProposalInteractionListener mBridgeListener;
    @InjectView(R.id.proposal)
    Button proposal;
    @InjectView(R.id.complaint)
    Button complaint;
    @InjectView(R.id.et_info)
    EditText info;
    @InjectView(R.id.btn_recorder)
    AudioRecorderButton recorderButton;

    TextView app_upload_image, app_complaint, app_cancle;

    private ProposalSourceType sourceType = ProposalSourceType.UNKNOWN;
    private ProposalMediaType msg_type = ProposalMediaType.TEXT;           //1:文本，2：语音，3：图片
    private ProposalMessageCategory category = ProposalMessageCategory.SUGGESTION;           //0：投诉；1：建议；2：BUG
    private int duration = 0;       //录音时长
    private View mPopView;
    private PopupWindow mPopupWindow;
    private WindowManager.LayoutParams params;

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

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);

        if (DMApplication.getInstance().iAmUser()) {
            redirectToLoadUrl(Constants.kWebpageUserCenter);
            sourceType = ProposalSourceType.FROM_USER;
        } else {
            redirectToLoadUrl(Constants.kWebpageAgencyCenter);
            sourceType = ProposalSourceType.FROM_AGENCY;
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        proposal.performClick();

        this.params = getActivity().getWindow().getAttributes();

        initPopupView();
        initPopupViewConfig();
        initOnClickListener();
    }

    private void initOnClickListener() {
        //TODO 监听软键盘回车按钮
//        info.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_SEND
//                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
//
//                    if (TextUtils.isEmpty(info.getText()))
//                        return false;
//                    Map<String, Object> params = new HashMap<>();
//                    msg_type = ProposalMediaType.TEXT;
//                    params.put("msg_type", msg_type);
//                    params.put("type", sourceType);
//                    params.put("content", info.getText().toString());
//                    params.put("complain_agency_id", 0);
//                    params.put("category", category);
//                    params.put("duration", duration);
//                    //触发软键盘回车事件，上传服务器;
//                    try {
//                        OkHttpUtils.postString().url(Constants.kWebServiceSendFeedback)
//                                .addHeader("Content-Type", "application/json")
//                                .addHeader("token", getUserToken())
//                                .content(com.alibaba.fastjson.JSONObject.toJSONString(params))
//                                .mediaType(okhttp3.MediaType.parse("application/json"))
//                                .build()
//                                .execute(new StringCallback() {
//
//                                    @Override
//                                    public void onError(Call call, Exception e) {
//
//                                    }
//
//                                    @Override
//                                    public void onResponse(String response) {
//                                        try {
//                                            new JSONObject(response);
//                                            ToastUtil.showShort(getContext(), "提交成功");
//                                        } catch (JSONException e) {
//                                            ToastUtil.showShort(getContext(), "提交失败");
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                });
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    return true;
//                }
//                return false;
//            }
//        });

        recorderButton.setAudioFinishRecorderListener(new AudioRecorderButton.AudioFinishRecorderListener() {
            @Override
            public void onFinish(float seconds, String filePath) {
                // Upload recorded audio to backend
                msg_type = ProposalMediaType.AUDIO;
                mBridgeListener.onUploadProposalAudio(filePath);
            }
        });
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
    }

    private void initPopupViewConfig() {
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
                mBridgeListener.onSelectImageForProposal();
            }
        });

        app_complaint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopupWindow();
            }
        });
    }

    @OnClick({R.id.proposal, R.id.complaint, R.id.btn_other})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.proposal:
                proposal.setEnabled(false);
                complaint.setEnabled(true);
                category = ProposalMessageCategory.SUGGESTION;
                break;
            case R.id.complaint:
                proposal.setEnabled(true);
                complaint.setEnabled(false);
                category = ProposalMessageCategory.COMPLAINT;
                break;
            case R.id.btn_other:
                mPopupWindow.showAtLocation(mWebView, Gravity.BOTTOM, 0, 0);
                mPopupWindow.setAnimationStyle(R.style.app_pop);
                mPopupWindow.setOutsideTouchable(true);
                mPopupWindow.setFocusable(true);
                mPopupWindow.update();
                params.alpha = 0.7f;
                setWindowAttributes();
                break;
        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if (requestCode == SYS_INTENT_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
//            Uri uri = data.getData();
//            try {
//                msg_type = ProposalMediaType.IMAGE;
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
//                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
//                String fileName = formatter.format(System.currentTimeMillis()) + ".jpg";
//                bitmap = ImageUtil.getInstance().compress(bitmap);
//                FileUtil.saveMyBitmap(FileUtil.getWaterPhotoPath(), fileName, bitmap);
//                //上传至服务器
//                uploadFile(FileUtil.getWaterPhotoPath() + fileName);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public interface OnProposalInteractionListener extends WebViewBaseFragment.OnBridgeInteractionListener {
        void onUploadProposalAudio(String data);

        void onSelectImageForProposal();
    }

//    private class UploadResultCallBack extends OkHttpClientManager.ResultCallback<String> {
//        @Override
//        public void onError(Request request, Exception e) {
//
//        }
//
//        @Override
//        public void onResponse(String response) {
//            Log.i("response", response);
//            try {
//                JSONArray jsonArray = new JSONArray(response);
//                JSONObject jsonObject = jsonArray.getJSONObject(0);
//                String responseUrl = jsonObject.getJSONArray("original").getString(0);
//                Map<String, Object> params = new HashMap<>();
//                params.put("msg_type", msg_type);
//                params.put("type", sourceType);
//                params.put("content", responseUrl);
//                params.put("complain_agency_id", 0);
//                params.put("category", category);
//                params.put("duration", duration);
//                OkHttpClientManager.postJson(Constants.kWebServiceSendFeedback,
//                        com.alibaba.fastjson.JSONObject.toJSONString(params),
//                        getUserToken(),
//                        new SendFeedBackCallBack());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private class SendFeedBackCallBack implements Callback {
//
//        @Override
//        public void onFailure(Request request, IOException e) {
//            Message message = handler.obtainMessage();
//            message.what = TIMEOUT;
//            handler.sendMessage(message);
//        }
//
//        @Override
//        public void onResponse(Response response) throws IOException {
//            Message message = handler.obtainMessage();
//            message.what = SUCCESS;
//            if (response.isSuccessful())
//                message.arg1 = 1;
//            else
//                message.arg1 = 0;
//            handler.sendMessage(message);
//        }
//    }

}
