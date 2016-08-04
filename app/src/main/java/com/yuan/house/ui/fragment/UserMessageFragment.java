package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.house.R;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/22.
 */
public class UserMessageFragment extends WebViewBaseFragment {
    @BindView(R.id.rightItem)
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

        ButterKnife.bind(this, view);

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
                    if ("text".equals(object.getString("type")) && tvRightItem != null) {
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

                JSONObject object = new JSONObject();
                JSONObject innerObject = new JSONObject();
                try {
                    innerObject.put("title", "通讯录");
                    innerObject.put("hasBackButton", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    object.put("params", innerObject);
                    mBridgeListener.onBridgeOpenNewLink(url, object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.sortBy:
                getBridge().callHandler(Constants.kJavascriptFnOnRightItemClick);
                break;
        }
    }
}
