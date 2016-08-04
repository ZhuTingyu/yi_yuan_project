package com.yuan.house.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dimo.network.Connectivity;
import com.yuan.house.R;
import com.yuan.house.utils.ToastUtil;

import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/4/16.
 */

public class ConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.v("Connection changed");

        // TODO: 8/4/16 do postpone stuff based on connection change
        if (!Connectivity.isConnected(context)) {
            ToastUtil.showShort(context, R.string.error_connection_failed);
        } else {
            ToastUtil.showShort(context, R.string.error_connection_okay);
        }
    }
}
