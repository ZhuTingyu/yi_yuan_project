package com.yuan.house.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.avoscloud.chat.util.Utils;
import com.dimo.utils.PackageUtil;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.common.Constants;
import com.yuan.house.event.NotificationEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 16/6/30.
 */

public class PushReceiver extends BroadcastReceiver {
    Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        try {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (extras == null) return;
            String channel = extras.getString("com.avos.avoscloud.Channel");
            //获取消息内容
            String data = extras.getString("com.avos.avoscloud.Data");
            if (TextUtils.isEmpty(data)) return;

            JSONObject json = new JSONObject(data);

            Timber.d("got action " + action + " on channel " + channel + " with:");
            Iterator itr = json.keys();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                Timber.d("..." + key + " => " + json.getString(key));
            }

            // handle push notification and dispatch
            dispatch(json);
        } catch (JSONException e) {
            Timber.d("JSONException: " + e.getMessage());
        }
    }

    /**
     * Dispatch dispatch handler based on different message types
     *
     * @param object notification payload
     */
    private void dispatch(JSONObject object) {
        int msgType = object.optInt("msg_type");
        String raw = object.optString("holder");

        JSONObject holder;
        try {
            holder = new JSONObject(raw);

            EventBus.getDefault().post(NotificationEvent.fromType(msgType, holder));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (msgType == NotificationEvent.NotificationEventEnum.NOTICE_MESSAGE.getValue()) {
            Utils.notifyMsg(mContext, MainActivity.class, PackageUtil.getAppLable(mContext), null, object.optString("alert"), Constants.kNotifyId);
        }
    }
}
