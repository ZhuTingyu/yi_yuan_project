package com.yuan.house.event;

/**
 * Created by Alsor Zhou on 16/6/6.
 */

public class AuthEvent {
    AuthEventEnum eventType;

    String holder;

    public AuthEvent(AuthEventEnum eventType, String holder) {
        this.eventType = eventType;
        this.holder = holder;
    }

    public AuthEventEnum getEventType() {
        return eventType;
    }

    public String getHolder() {
        return holder;
    }

    public enum AuthEventEnum {
        LOGOUT(1), NEED_LOGIN_AGAIN(2);

        private int value;

        AuthEventEnum(int value) {
            this.value = value;
        }
    }
}

