package com.yuan.house.event;

import org.json.JSONObject;

/**
 * Created by Alsor Zhou on 16/6/6.
 */

public class NotificationEvent {
    NotificationEventEnum eventType;

    JSONObject holder;

    public NotificationEvent(NotificationEventEnum eventType, JSONObject holder) {
        this.eventType = eventType;
        this.holder = holder;
    }

    public NotificationEventEnum getEventType() {
        return eventType;
    }

    public JSONObject getHolder() {
        return holder;
    }

    public static NotificationEvent fromType(int value, JSONObject payload) {
        int index = value - 1;

        NotificationEventEnum type = NotificationEventEnum.values()[index];

        return new NotificationEvent(type, payload);
    }

    public enum NotificationEventEnum {
        NOTICE_MESSAGE(1),
        NEW_TRANSACTION(2),
        BBS_MESSAGE(3),
        HOUSE_RECOMMENDED_MESSAGE(4),
        NEW_HOUSE_AUDIT(5),
        NEW_EXCLUSIVE_CONTRACT(6),
        NEW_PREORDER_CONTRACT(7),
        NEW_BUSINESS_CONTRACT(8),
        NEW_AGENCY_TRANSATION(9),
        KICK_OUT(10);

        private int value;

        NotificationEventEnum(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}

