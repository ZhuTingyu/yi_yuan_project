package com.yuan.house.event;

/**
 * Created by yang on 15/12/31.
 */
public class WebBroadcastEvent {

    public Object eventSource;//事件抛出者
    public String result;

    public WebBroadcastEvent(String result, Object eventSource) {
        this.result = result;
        this.eventSource = eventSource;
    }

}
