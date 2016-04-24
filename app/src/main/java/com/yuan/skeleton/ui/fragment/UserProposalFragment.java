package com.yuan.skeleton.ui.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.yuan.skeleton.R;
import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.utils.JsonParse;

import org.json.JSONException;

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
    private View mPopView;
    private PopupWindow mPopupWindow;
    TextView app_upload_image,app_complaint,app_cancle;

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
            if(JsonParse.getInstance().judgeUserType())
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
        initPopupView();
        initPopupViewConfig();
    }

    private void initPopupView(){
        mPopView = LayoutInflater.from(getContext()).inflate(R.layout.popup_other,null);
        app_cancle=(TextView) mPopView.findViewById(R.id.app_cancle);
        app_upload_image=(TextView) mPopView.findViewById(R.id.app_upload_image);
        app_complaint=(TextView) mPopView.findViewById(R.id.app_complaint);
    }

    private void initPopupViewConfig(){
        mPopupWindow=new PopupWindow(mPopView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
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

    @OnClick({R.id.proposal,R.id.complaint,R.id.btn_other})
    public void onClick(View view){
        switch (view.getId()){
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

                WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
                params.alpha = 0.7f;
                getActivity().getWindow().setAttributes(params);
                break;
        }
    }

    private void closePopupWindow(){
        if(mPopupWindow != null && mPopupWindow.isShowing()){
            mPopupWindow.dismiss();
            WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
            params.alpha = 1;
            getActivity().getWindow().setAttributes(params);
        }
    }

}
