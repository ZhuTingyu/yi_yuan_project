package com.yuan.skeleton.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.avoscloud.chat.entity.avobject.User;
import com.avoscloud.chat.service.UserService;
import com.avoscloud.leanchatlib.utils.NetAsyncTask;
import com.dimo.http.RestClient;
import com.dimo.web.WebViewJavascriptBridge;

import com.yuan.skeleton.R;
import com.yuan.skeleton.common.Constants;
import com.yuan.skeleton.ui.fragment.WebViewFragment;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

/**
 * Created by yj on 2015/8/27.
 */
public class SignInActivity extends WebViewBasedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebViewFragment fragment = new WebViewFragment();
        Bundle arguments = new Bundle();
        arguments.putString("url", "login.html");

        findViewById(R.id.topbar_left_btn).setVisibility(View.GONE);
        findViewById(R.id.topbar_left_textbtn).setVisibility(View.GONE);
        setTitleItem(R.string.sign_in_title);

        fragment.setArguments(arguments);
        mFragmentTransaction.replace(R.id.content_frame, fragment);
        mFragmentTransaction.commit();
    }

    @Override
    protected void registerHandle() {
        super.registerHandle();

        bridge.registerHandler("signin", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(final String data, WebViewJavascriptBridge.WVJBResponseCallback callback) {
                try {
                    final JSONObject userLogin = new JSONObject(data);
                    final String chat_service_id = userLogin.has("chat_service_id") ? userLogin.getString("chat_service_id") : null;
                    final String chat_service_passwd = userLogin.has("chat_service_passwd") ? userLogin.getString("chat_service_passwd") : null;
                    if (TextUtils.isEmpty(chat_service_id)) {
                        Timber.e("Empty");
                    } else {

                    }

                    if(userLogin.isNull("chat_user_id") || TextUtils.isEmpty(userLogin.getString("chat_user_id"))) {
                        NetAsyncTask task = new NetAsyncTask(SignInActivity.this) {
                            @Override
                            protected void doInBack() throws Exception {
                                AVUser user = UserService.signUp(chat_service_id, chat_service_passwd);
                                User.setGender(user, userLogin.getInt("gender")==1? User.Gender.Male:User.Gender.Female);
                                user.setFetchWhenSave(true);
                                user.save();
                            }

                            @Override
                            protected void onPost(Exception e) {
                                if (e != null) {
                                    Timber.e(e, "failed on register av user");
                                } else {
                                    UserService.updateUserLocation();
                                }
                                final AVUser avUser = AVUser.getCurrentUser();
                                try {
                                    JSONObject request = new JSONObject();
                                    request.put("url", "/user/"+userLogin.getInt("id"));
                                    request.put("headers", new JSONObject("{\"access-key\":\"" + userLogin.getString("access_key") + "\"}"));
                                    request.put("data", new JSONObject("{\"chat_user_id\":\"" + avUser.getObjectId() + "\"}"));
                                    RestClient.getInstance().bridgeRequest(request, RestClient.METHOD_PUT, new WebViewJavascriptBridge.WVJBResponseCallback() {
                                        @Override
                                        public void callback(String data) {
                                            try {
                                                JSONObject response = new JSONObject(data);
                                                if(response.getInt("status")==200) {
                                                    userLogin.put("chat_user_id", avUser.getObjectId());
                                                    prefs.edit().putString(Constants.kLeanChatCurrentUserObjectId, avUser.getObjectId()).commit();
                                                    prefs.edit().putString("userLogin", userLogin.toString()).commit();
                                                    startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                                    finish();
                                                }
                                            } catch (JSONException e1) {
                                                e1.printStackTrace();
                                            }
                                            Timber.i("upload chat_user_id response:" + data);
                                        }
                                    });
                                } catch (Exception e1) {
                                    Timber.e(e1, "error on upload chat_user_id");
                                }
                            }
                        };

                        task.execute();
                    }else {

                        //TODO: handler after login to own server success
                        AVUser.logInInBackground(chat_service_id,
                                chat_service_passwd,
                                new LogInCallback<AVUser>() {
                                    @Override
                                    public void done(AVUser avUser, AVException e) {
                                        if (avUser != null) {
                                            String chatUserId = avUser.getObjectId();
                                            prefs.edit().putString("userLogin", data).apply();
                                            prefs.edit().putString(Constants.kLeanChatCurrentUserObjectId, chatUserId).commit();
                                            UserService.updateUserLocation();
                                        }

                                        startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                        finish();
                                    }
                                });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
