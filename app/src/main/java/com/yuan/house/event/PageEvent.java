package com.yuan.house.event;

/**
 * Created by Alsor Zhou on 6/11/15.
 */
public class PageEvent {
    PageEventEnum eventType;

    String holder;

    public PageEvent(PageEventEnum eventType, String holder) {
        this.eventType = eventType;
        this.holder = holder;
    }

    public PageEventEnum getEventType() {
        return eventType;
    }

    public String getHolder() {
        return holder;
    }

    public enum PageEventEnum {
        FINISHED(1), REDIRECT(2), FRIENDSHIP_UPDATE(5), DROP_TO_MESSAGE(6), GET_LOCATION(7), DROP_TO_CENTER(8);

        private int value;

        PageEventEnum(int value) {
            this.value = value;
        }
    }

}
