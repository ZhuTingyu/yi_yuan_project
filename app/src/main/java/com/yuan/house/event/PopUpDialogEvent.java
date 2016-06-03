package com.yuan.house.event;

/**
 * Created by Alsor Zhou on 5/7/15.
 */
public class PopUpDialogEvent {
    String type;

    public PopUpDialogEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
