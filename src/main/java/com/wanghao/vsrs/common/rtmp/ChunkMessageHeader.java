package com.wanghao.vsrs.common.rtmp;

/**
 * @author wanghao
 */
public class ChunkMessageHeader {

    /** Chunk Message Header */
    // 3-byte
    private int timestampDelta;
    // 3-byte
    private int payloadLength;
    // 1-byte
    private byte messageTypeId;
    // 4-byte, little-endian
    private int messageStreamId;

    // calculated timestamp
    private long timestamp;


    public int getTimestampDelta() {
        return timestampDelta;
    }

    public void setTimestampDelta(int timestampDelta) {
        this.timestampDelta = timestampDelta;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public byte getMessageTypeId() {
        return messageTypeId;
    }

    public void setMessageTypeId(byte messageTypeId) {
        this.messageTypeId = messageTypeId;
    }

    public int getMessageStreamId() {
        return messageStreamId;
    }

    public void setMessageStreamId(int messageStreamId) {
        this.messageStreamId = messageStreamId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
