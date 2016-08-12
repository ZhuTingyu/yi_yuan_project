package com.yuan.house.event;

/**
 * Created by Alsor Zhou on 16/6/6.
 */

public class NetworkReachabilityEvent {
    NetworkReachabilityEventEnum eventType;

    String holder;

    public NetworkReachabilityEvent(NetworkReachabilityEventEnum eventType, String holder) {
        this.eventType = eventType;
        this.holder = holder;
    }

    public NetworkReachabilityEventEnum getEventType() {
        return eventType;
    }

    public String getHolder() {
        return holder;
    }

    public enum NetworkReachabilityEventEnum {
        ONLINE(1), OFFLINE(2);

        private int value;

        NetworkReachabilityEventEnum(int value) {
            this.value = value;
        }
    }
}

