package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/22.
 */
public class UserMessageFragment extends WebViewBaseFragment {
    @InjectView(R.id.rightItem)
    TextView tvRightItem;

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

        redirectToLoadUrl(Constants.kWebpageUserMessage);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        registerHandle();
    }

    @Override
    protected void registerHandle() {
        super.registerHandle();

        bridge.registerHandler("setRightItem", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                try {
                    JSONObject object = new JSONObject(data);
                    if ("text".equals(object.getString("type"))) {
                        tvRightItem.setText(object.getString("content"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @OnClick({R.id.contacts, R.id.sortBy})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contacts:
                String url = "user_contacts.html";
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("params", "{\"title\":\"通讯录\",\"hasBackButton\":true}");
//                ((MainActivity)getActivity()).openLinkInNewActivity(url,map);
                mBridgeListener.onBridgeOpenNewLink(url, map);
                break;
            case R.id.sortBy:
                getBridge().callHandler(Constants.kJavascriptFnOnRightItemClick);
                break;
        }
    }
}
