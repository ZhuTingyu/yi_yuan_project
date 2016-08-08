package com.avoscloud.chat.ui.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.callback.AVIMConversationQueryCallback;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationManager;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.dimo.helper.ViewHelper;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
import com.yuan.house.common.Constants;
import com.yuan.house.http.RestClient;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alsor Zhou on 16/7/27.
 */

public class ServiceChatActivity extends ChatActivity {

    private static final String ENABLED = "ENABLED";
    private ScheduledExecutorService scheduledExecutor;

    public static void chatByConversation(final Context from, String conversation_id, final boolean isOpen) {
        List<String> convIds = new ArrayList<>();
        convIds.add(conversation_id);

        ConversationManager.getInstance().findConversationsByConversationIds(convIds, new AVIMConversationQueryCallback() {
            @Override
            public void done(List<AVIMConversation> list, AVIMException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                if (list == null) {
                    return;
                }

                AVIMConversation conv = list.get(0);

                CacheService.registerConv(conv);

                ChatManager.getInstance().registerConversation(conv);

                Intent intent = new Intent(from, ServiceChatActivity.class);
                intent.putExtra(CONVID, conv.getConversationId());
                intent.putExtra(ENABLED, isOpen);

                from.startActivity(intent);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitleItem(R.string.title_service_chat);

        Bundle bundle = getIntent().getExtras();

        boolean isOpen = bundle.getBoolean(ENABLED);

        ViewHelper.setViewAndChildrenEnabled(findViewById(R.id.bottomLayout), isOpen);

        getServiceQueueLength();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
    }

    private void getServiceQueueLength() {
        BDLocation location = DMApplication.getInstance().getLastActivatedLocation();

        RequestParams requestParams = new RequestParams();
        requestParams.put("city", location.getCity());
        requestParams.put("district", location.getDistrict());

        RestClient.getInstance().get("/service-queue/length", null, requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                if (statusCode == 200) {
                    String count = response.optString("waiting_count");
                    promptTheWaitingQueue(Integer.parseInt(count));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    private void promptTheWaitingQueue(int i) {
        if (i == 0) {
            i++;
        }

        String msg = String.format(Constants.kForceLocale, "当前有 %d 位用户在排队，你是否等待？", i);
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(R.string.wait, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startEnterQueue();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();
    }

    private void startEnterQueue() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        long kTickForQueueTimer = 110;
        scheduledExecutor.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        enterQueue();
                    }
                }, 0, kTickForQueueTimer, TimeUnit.SECONDS);
    }

    private void enterQueue() {
        BDLocation location = DMApplication.getInstance().getLastActivatedLocation();

        RequestParams requestParams = new RequestParams();
        requestParams.put("city", location.getCity());
        requestParams.put("district", location.getDistrict());

        RestClient.getInstance().post("/service-conversation/queue", null, requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                int count = response.optInt("waiting_count");

                if (statusCode == 200) {
                    setupWaitNumberView(count);
                } else if (statusCode == 400) {
                    customerServiceIsReady();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    private void setupWaitNumberView(int count) {
        String msg = String.format(Constants.kForceLocale, "当前排队人数是 %d 位", count);

        findViewById(R.id.lvMessages).setVisibility(View.GONE);

        TextView placeholder = (TextView) findViewById(R.id.placeholder_service_wait);
        placeholder.setVisibility(View.VISIBLE);
        placeholder.setText(msg);
    }

    private void customerServiceIsReady() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }

        ViewHelper.setViewAndChildrenEnabled(findViewById(R.id.bottomLayout), true);

        findViewById(R.id.lvMessages).setVisibility(View.VISIBLE);

        TextView placeholder = (TextView) findViewById(R.id.placeholder_service_wait);
        placeholder.setVisibility(View.GONE);
    }
}
