package com.yuan.skeleton.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuan.skeleton.R;
import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.activities.MapActivity;
import com.yuan.skeleton.application.Injector;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/22.
 */
public class UserMessageFragment extends WebViewBaseFragment {

    public static UserMessageFragment newInstance() {
        UserMessageFragment fragment = new UserMessageFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_message_user, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);

        redirectToLoadUrl("user_message.html");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @OnClick({R.id.book})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.book:
                String url = "user_contacts.html";
                HashMap<String,String> map = new HashMap<String, String>();
                map.put("params","{\"title\":\"通讯录\",\"hasBackButton\":true}");
                ((MainActivity)getActivity()).openLinkInNewActivity(url,map);
                break;
        }
    }


}
