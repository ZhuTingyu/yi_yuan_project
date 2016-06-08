package com.yuan.house.ui.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.dimo.utils.StringUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.application.DMApplication;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.ui.view.AudioRecorderButton;
import com.yuan.house.utils.FileUtil;
import com.yuan.house.utils.ImageUtil;
import com.yuan.house.utils.OkHttpClientManager;
import com.yuan.house.utils.ToastUtil;
import com.yuan.skeleton.R;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import okhttp3.Call;

/**
 * 用户端和中介端合并在一个Fragment里
 * Created by KevinLee on 2016/4/24.
 */
public class UserProposalFragment extends WebViewBaseFragment {

    private static final int SUCCESS = 1;
    private static final int TIMEOUT = 0;
    private final OkHttpClient client = new OkHttpClient();
    private final int SYS_INTENT_REQUEST = 0XFF01;
    @InjectView(R.id.proposal)
    Button proposal;
    @InjectView(R.id.complaint)
    Button complaint;
    @InjectView(R.id.et_info)
    EditText info;
    @InjectView(R.id.btn_recorder)
    AudioRecorderButton recorderButton;
    TextView app_upload_image, app_complaint, app_cancle;
    private int type = 0;       //1:用户发的，2：中介发的
    private int msg_type = 0;           //1:文本，2：语音，3：图片
    private int category = 1;           //0：投诉；1：建议；2：BUG
    private int duration = 0;       //录音时长
    private View mPopView;
    private PopupWindow mPopupWindow;
    private WindowManager.LayoutParams params;
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TIMEOUT:
                    ToastUtil.showShort(getContext(), "发送超时");
                    break;
                case SUCCESS:
                    if (msg.arg1 == 1)
                        ToastUtil.showShort(getContext(), "发送成功");
                    else
                        ToastUtil.showShort(getContext(), "发送失败");
                    break;
            }
        }
    };

    public static UserProposalFragment newInstance() {
        UserProposalFragment fragment = new UserProposalFragment();
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
            type = 1;
        } else {
            redirectToLoadUrl(Constants.kWebpageAgencyCenter);
            type = 2;
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
        info.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                    if (TextUtils.isEmpty(info.getText()))
                        return false;
                    Map<String, Object> params = new HashMap<>();
                    msg_type = 1;
                    params.put("msg_type", msg_type);
                    params.put("type", type);
                    params.put("content", info.getText().toString());
                    params.put("complain_agency_id", 0);
                    params.put("category", category);
                    params.put("duration", duration);
                    //触发软键盘回车事件，上传服务器;
                    try {
                        OkHttpUtils.postString().url(Constants.kWebServiceSendFeedback)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("token", getUserToken())
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
                                            new JSONObject(response);
                                            ToastUtil.showShort(getContext(), "提交成功");
                                        } catch (JSONException e) {
                                            ToastUtil.showShort(getContext(), "提交失败");
                                            e.printStackTrace();
                                        }
                                    }
                                });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

        recorderButton.setAudioFinishRecorderListener(new AudioRecorderButton.AudioFinishRecorderListener() {
            @Override
            public void onFinish(float seconds, String filePath) {
                final String path = filePath;
                //上传到服务端
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            msg_type = 2;
                            uploadFile(path);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });

    }

    /**
     * 上传文件
     *
     * @param filePath
     */
    private void uploadFile(String filePath) throws JSONException {
        String token = getUserToken();
        try {
            if (msg_type == 2)
                this.duration = MediaPlayer.create(getActivity(), Uri.parse(filePath)).getDuration();

            File file = new File(filePath);
            OkHttpUtils.post().url(Constants.kWebServiceFileUpload)
                    .addHeader("token", token)
                    .addHeader("Content-Type", "multipart/form-data")
                    .addFile("file[]", file.getName(), file)
                    .build()
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e) {

                        }

                        @Override
                        public void onResponse(String response) {
                            try {
                                new JSONObject(response);
                                ToastUtil.showShort(getContext(), "提交成功");
                            } catch (JSONException e) {
                                ToastUtil.showShort(getContext(), "提交失败");
                                e.printStackTrace();
                            }
                        }
                    });

            /*
            OkHttpClientManager.postAsyn(Constants.kWebServiceFileUpload,
                    new UploadResultCallBack(),
                    new File[]{new File(filePath)},
                    new String[]{"file[]"},
                    new OkHttpClientManager.Param[]{
                            new OkHttpClientManager.Param("token", token),
                            new OkHttpClientManager.Param("Content-Type", "multipart/form-data")});*/
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getUserToken() throws JSONException {
        String json = ((MainActivity) getActivity()).prefs.getString("userLogin", "");
        HashMap<String, String> params = StringUtil.JSONString2HashMap(json);
        return params.get("token");
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
                systemPhoto();
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
                category = 1;
                break;
            case R.id.complaint:
                proposal.setEnabled(true);
                complaint.setEnabled(false);
                category = 0;
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

    /**
     * 打开系统相册
     */
    public void systemPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SYS_INTENT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SYS_INTENT_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                msg_type = 3;
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                String fileName = formatter.format(System.currentTimeMillis()) + ".jpg";
                bitmap = ImageUtil.getInstance().compressImage(bitmap);
                FileUtil.saveMyBitmap(FileUtil.getWaterPhotoPath(), fileName, bitmap);
                //上传至服务器
                uploadFile(FileUtil.getWaterPhotoPath() + fileName);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class UploadResultCallBack extends OkHttpClientManager.ResultCallback<String> {
        @Override
        public void onError(Request request, Exception e) {

        }

        @Override
        public void onResponse(String response) {
            Log.i("response", response);
            try {
                JSONArray jsonArray = new JSONArray(response);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                String responseUrl = jsonObject.getJSONArray("original").getString(0);
                Map<String, Object> params = new HashMap<>();
                params.put("msg_type", msg_type);
                params.put("type", type);
                params.put("content", responseUrl);
                params.put("complain_agency_id", 0);
                params.put("category", category);
                params.put("duration", duration);
                OkHttpClientManager.postJson(Constants.kWebServiceSendFeedback,
                        com.alibaba.fastjson.JSONObject.toJSONString(params),
                        getUserToken(),
                        new SendFeedBackCallBack());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendFeedBackCallBack implements Callback {

        @Override
        public void onFailure(Request request, IOException e) {
            Message message = handler.obtainMessage();
            message.what = TIMEOUT;
            handler.sendMessage(message);
        }

        @Override
        public void onResponse(Response response) throws IOException {
            Message message = handler.obtainMessage();
            message.what = SUCCESS;
            if (response.isSuccessful())
                message.arg1 = 1;
            else
                message.arg1 = 0;
            handler.sendMessage(message);
        }
    }
}
