package com.yuan.skeleton.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.yuan.skeleton.R;
import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.common.Constants;
import com.yuan.skeleton.ui.view.AudioRecorderButton;
import com.yuan.skeleton.utils.JsonParse;
import com.yuan.skeleton.utils.ToastUtil;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * 用户端和中介端合并在一个Fragment里
 * Created by KevinLee on 2016/4/24.
 */
public class UserProposalFragment extends WebViewBaseFragment {

    @InjectView(R.id.proposal)
    Button proposal;
    @InjectView(R.id.complaint)
    Button complaint;
    @InjectView(R.id.et_info)
    EditText info;
    @InjectView(R.id.btn_recorder)
    AudioRecorderButton recorderButton;

    private View mPopView;
    private PopupWindow mPopupWindow;
    private WindowManager.LayoutParams params;
    TextView app_upload_image, app_complaint, app_cancle;

    private final OkHttpClient client = new OkHttpClient();

    public static UserProposalFragment newInstance() {
        UserProposalFragment fragment = new UserProposalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_proposal_user, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);
        try {
            //TODO 根据登陆账户加载不同的页面
            if (JsonParse.getInstance().judgeUserType())
                redirectToLoadUrl("user_center.html");
            else
                redirectToLoadUrl("agency_center.html");
        } catch (JSONException e) {
            e.printStackTrace();
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
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    //触发软键盘回车事件，上传服务器;
                    ToastUtil.showShort(getContext(), "监听到回车键啦");
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
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), new File(filePath));
        RequestBody requestBody = new MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addPart(Headers.of("Content-Disposition","form-data"),fileBody)
                .build();

        Request request = new Request.Builder()
                .url(Constants.kWebServiceFileUpload)
                .addHeader("Content-Type","multipart/form-data")
                .addHeader("token",token)
                .post(requestBody)
                .build();

        Headers headers = request.headers();
        Log.i("NativeHeaders",headers.toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.i("uploadresponse",e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                Log.i("uploadresponse",response.isSuccessful() + "");
                Log.i("responseCode",response.code() + "");
                Log.i("responseBody",response.body().string() + "");
                Log.i("responseHeader",response.headers().toString());
            }
        });

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
                break;
            case R.id.complaint:
                proposal.setEnabled(true);
                complaint.setEnabled(false);
                break;
            case R.id.btn_other:
                mPopupWindow.showAtLocation(webView, Gravity.BOTTOM, 0, 0);
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

}
