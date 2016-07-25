package com.yuan.house.event;

/**
 * Created by yang on 15/12/31.
 */
public class WebBroadcastEvent {
    Object source;//事件抛出者
    String payload;

    public Object getSource() {
        return source;
    }

    public String getPayload() {
        return payload;
    }

    public WebBroadcastEvent(String result, Object eventSource) {
        this.payload = result;
        this.source = eventSource;
    }

}
