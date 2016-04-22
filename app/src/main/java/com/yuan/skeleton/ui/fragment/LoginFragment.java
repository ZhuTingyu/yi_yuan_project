package com.yuan.skeleton.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avoscloud.chat.entity.avobject.User;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.leanchatlib.utils.NetAsyncTask;
import com.dimo.http.RestClient;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.skeleton.R;
import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.application.Injector;
import com.yuan.skeleton.common.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by KevinLee on 2016/4/22.
 */
public class LoginFragment extends WebViewBaseFragment {

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_login, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);

        redirectToLoadUrl("login.html");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
