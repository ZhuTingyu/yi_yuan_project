package com.yuan.house.receiver;

import com.yuan.house.event.NetworkReachabilityEvent;

import net.gotev.hostmonitor.HostMonitorBroadcastReceiver;
import net.gotev.hostmonitor.HostStatus;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 8/4/16.
 */

public class ReachabilityReceiver extends HostMonitorBroadcastReceiver {
    @Override
    public void onHostStatusChanged(HostStatus status) {
        Timber.w("onHostStatusChanged : %s", status.toString());

        if (status.isReachable() != status.isPreviousReachable()) {
            EventBus.getDefault().post(new NetworkReachabilityEvent(NetworkReachabilityEvent.NetworkReachabilityEventEnum.ONLINE, null));
        } else {
            EventBus.getDefault().post(new NetworkReachabilityEvent(NetworkReachabilityEvent.NetworkReachabilityEventEnum.OFFLINE, null));
        }
    }
}
