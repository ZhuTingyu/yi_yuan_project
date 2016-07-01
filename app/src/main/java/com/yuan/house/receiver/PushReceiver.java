package com.yuan.house.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.avoscloud.chat.util.Utils;
import com.dimo.utils.PackageUtil;
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
    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            String action = intent.getAction();
            String channel = intent.getExtras().getString("com.avos.avoscloud.Channel");
            //获取消息内容
            JSONObject json = new JSONObject(intent.getExtras().getString("com.avos.avoscloud.Data"));

            Timber.d("got action " + action + " on channel " + channel + " with:");
            Iterator itr = json.keys();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                Timber.d("..." + key + " => " + json.getString(key));
            }

            JSONObject object = json.optJSONObject("holder");
            Utils.notifyMsg(context, null, PackageUtil.getAppLable(context), null, json.optString("alert"), Constants.kNotifyId);

            // handle push notification and dispatch
            dispatch(object);
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
        JSONObject holder = object.optJSONObject("holder");

        EventBus.getDefault().post(NotificationEvent.fromType(msgType, holder));
    }
}
