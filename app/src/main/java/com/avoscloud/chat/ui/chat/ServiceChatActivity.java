package com.avoscloud.chat.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.callback.AVIMConversationQueryCallback;
import com.avoscloud.chat.service.CacheService;
import com.avoscloud.chat.service.ConversationManager;
import com.avoscloud.leanchatlib.activity.ChatActivity;
import com.avoscloud.leanchatlib.controller.ChatManager;
import com.baidu.location.BDLocation;
import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.yuan.house.R;
import com.yuan.house.application.DMApplication;
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

    private ScheduledExecutorService scheduledExecutor;

    public static void chatByConversation(final Context from, String conversation_id, boolean isOpen) {
        List<String> convIds = new ArrayList<>();
        convIds.add(conversation_id);

        ConversationManager.getInstance().findConversationsByConversationIds(convIds, new AVIMConversationQueryCallback() {
            @Override
            public void done(List<AVIMConversation> list, AVIMException e) {
                AVIMConversation conv = list.get(0);

                CacheService.registerConv(conv);

                ChatManager.getInstance().registerConversation(conv);
                Intent intent = new Intent(from, SingleChatActivity.class);
                intent.putExtra(CONVID, conv.getConversationId());

                from.startActivity(intent);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitleItem(R.string.title_service_chat);

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

                JSONObject data = response.optJSONObject("data");
                String count = data.optString("waiting_count");
                promptTheWaitingQueue(Integer.parseInt(count));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    private void promptTheWaitingQueue(int i) {
        final NiftyDialogBuilder dialogBuilder = NiftyDialogBuilder.getInstance(this);

        String msg = String.format("当前有 %d 位用户在排队，你是否等待？", i);

        dialogBuilder.withMessage(msg)
                .withDialogColor("#FFE74C3C")
                .withEffect(Effectstype.SlideBottom)
                .withButton1Text(getString(R.string.wait))
                .withButton2Text(getString(R.string.cancel))
                .withDuration(300)
                .setButton1Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogBuilder.dismiss();

                        startEnterQueue();
                    }
                })
                .setButton2Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogBuilder.dismiss();
                    }
                })
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

        RestClient.getInstance().post("/service-conversation/queue", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                JSONObject data = response.optJSONObject("data");
                int count = data.optInt("waiting_count");
                int status = response.optInt("status");

                if (status == 200) {
                    setupWaitNumberView(count);
                } else if (status == 400) {
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
        String msg = String.format("当前有 %d 位用户在排队，你是否等待？", count);

        // TODO: 16/7/28
    }

    private void customerServiceIsReady() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        // TODO: 16/7/28 okay to input
    }
}
