package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuan.skeleton.R;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.application.Injector;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/24.
 */
public class AgencyMessageFragment extends WebViewBaseFragment {

    public static AgencyMessageFragment newInstance() {
        AgencyMessageFragment fragment = new AgencyMessageFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_message_agency, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);

        redirectToLoadUrl("agency_message.html");

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
                String url = "agency_contacts.html";
                HashMap<String,String> map = new HashMap<String, String>();
                map.put("params","{\"title\":\"通讯录\",\"hasBackButton\":true}");
                ((MainActivity)getActivity()).openLinkInNewActivity(url,map);
                break;
        }
    }

}
