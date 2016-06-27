package com.yuan.house;

/**
 * Created by Alsor Zhou on 16/6/27.
 */

public enum HouseMessageType {
    HouseMessageType(0),
    UnsupportedMessageType(0),
    TextMessageType(-1),
    ImageMessageType(-2),
    AudioMessageType(-3),
    VideoMessageType(-4),
    LocationMessageType(-5),
    FileMessageType(-6);

    int type;

    private HouseMessageType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public static HouseMessageType getMessageType(int type) {
        switch(type) {
            case -6:
                return FileMessageType;
            case -5:
                return LocationMessageType;
            case -4:
                return VideoMessageType;
            case -3:
                return AudioMessageType;
            case -2:
                return ImageMessageType;
            case -1:
                return TextMessageType;
            case 0:
                return UnsupportedMessageType;
            case 2:
                return HouseMessageType;
            default:
                return UnsupportedMessageType;
        }
    }

}
