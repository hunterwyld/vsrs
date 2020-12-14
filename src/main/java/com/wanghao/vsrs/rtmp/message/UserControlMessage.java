package com.wanghao.vsrs.rtmp.message;

/**
 * @author wanghao
 * @description Used by RTMP Streaming Layer
 */
public class UserControlMessage extends RtmpMessage {
    // message stream id = 0 for control message
    private int messageStreamId = 0;

    // sent on chunk stream ID 2
    private int csid = 2;

    private short eventType;
    private int data;

    public UserControlMessage(short eventType, int data) {
        super(4);
        this.eventType = eventType;
        this.data = data;
    }

    public short getEventType() {
        return eventType;
    }

    public int getData() {
        return data;
    }

    public int getMessageStreamId() {
        return messageStreamId;
    }

    public int getCsid() {
        return csid;
    }
}
