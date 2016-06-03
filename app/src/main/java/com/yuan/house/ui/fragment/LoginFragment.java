package com.yuan.house.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.skeleton.R;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.application.Injector;
import com.yuan.house.common.Constants;
import com.yuan.house.utils.ToastUtil;

import org.json.JSONException;

import java.util.HashMap;

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

        ((MainActivity)getActivity()).bridge.registerHandler("setData", new WebViewJavascriptBridge.WVJBHandler() {

            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Timber.v("setData got:" + data);
                HashMap<String, String> params = null;
                try {
                    params = StringUtil.JSONString2HashMap(data);

                    String key = params.get("key");
                    String value = params.get("value");

                    if (value == null || value.equals("null"))
                        ((MainActivity)getActivity()).prefs.edit().remove(key).commit();
                    else
                        ((MainActivity)getActivity()).prefs.edit().putString(key, value).commit();

                    if("userLogin".equals(key)){
                        params = StringUtil.JSONString2HashMap(data);
                        params = StringUtil.JSONString2HashMap(params.get("value"));
                        if(params.get("user_info") != null)
                            params = StringUtil.JSONString2HashMap(params.get("user_info"));
                        else
                            params = StringUtil.JSONString2HashMap(params.get("agency_info"));
                        String userName = params.get("lean_user");
                        String passwd = params.get("lean_passwd");
                        avUserLogin(userName,passwd);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void avUserLogin(final String userName, String userPass){
        //TODO: handler after login to own server success
        AVUser.logInInBackground(userName,userPass,
                new LogInCallback<AVUser>() {
                    @Override
                    public void done(AVUser avUser, AVException e) {
                        if (avUser != null) {
                            String chatUserId = avUser.getObjectId();
                            ((MainActivity)getActivity()).prefs.edit().putString("avUserLogin", userName)
                                    .putString(Constants.kLeanChatCurrentUserObjectId, chatUserId)
                                    .apply();
                            UserService.updateUserLocation();
                            ChatManager chatManager = ChatManager.getInstance();
                            chatManager.setupDatabaseWithSelfId(AVUser.getCurrentUser().getObjectId());
                            chatManager.openClientWithSelfId(AVUser.getCurrentUser().getObjectId(), null);
                            CacheService.registerUser(AVUser.getCurrentUser());
                            ((MainActivity)getActivity()).prefs.edit().putBoolean("isLogin",true).commit();
                            ((MainActivity)getActivity()).switchToFragment(Constants.kFragmentTagNearby);
                            ((MainActivity)getActivity()).getBottomNavigationBar().clearAll();
                            ((MainActivity)getActivity()).setupTabbarClickListener();
                        }else {
                            ToastUtil.showShort(getContext(),"leancould登陆失败");
                        }
                    }
                });
    }

}
