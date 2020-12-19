package com.wanghao.vsrs.common.rtmp.message;

/**
 * @author wanghao
 * @description Used by RTMP Chunk Stream Protocol
 */
public class ProtocolControlMessage extends RtmpMessage {
    // message stream id = 0 for control message
    private int messageStreamId = 0;

    // sent on chunk stream ID 2
    private int csid = 2;

    public ProtocolControlMessage(int messageTypeId) {
        super(messageTypeId);
    }

    public int getMessageStreamId() {
        return messageStreamId;
    }

    public int getCsid() {
        return csid;
    }

}
