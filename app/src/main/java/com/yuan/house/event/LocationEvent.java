package com.yuan.house.event;

import com.baidu.location.BDLocation;

/**
 * Created by Alsor Zhou on 16/6/7.
 */

public class LocationEvent {
    LocationEventEnum eventType;

    BDLocation holder;

    public LocationEvent(LocationEventEnum eventType, BDLocation holder) {
        this.eventType = eventType;
        this.holder = holder;
    }

    public LocationEventEnum getEventType() {
        return eventType;
    }

    public BDLocation getHolder() {
        return holder;
    }

    public enum LocationEventEnum {
        UPDATED(1);

        private int value;

        LocationEventEnum(int value) {
            this.value = value;
        }
    }

}

