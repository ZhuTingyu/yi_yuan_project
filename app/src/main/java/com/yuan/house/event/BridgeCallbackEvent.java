package com.yuan.house.event;

/**
 * Created by Alsor Zhou on 16/6/27.
 */
public class BridgeCallbackEvent {
    BridgeCallbackEventEnum eventType;

    String holder;

    public BridgeCallbackEvent(BridgeCallbackEventEnum eventType, String holder) {
        this.eventType = eventType;
        this.holder = holder;
    }

    public BridgeCallbackEventEnum getEventType() {
        return eventType;
    }

    public String getHolder() {
        return holder;
    }

    public enum BridgeCallbackEventEnum {
        CALLBACK(1);

        private int value;

        BridgeCallbackEventEnum(int value) {
            this.value = value;
        }
    }
}

