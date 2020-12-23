package com.wanghao.vsrs.common.rtmp.message;

/**
 * @author wanghao
 * @description self-defined message type
 */
public class TextMessage extends RtmpMessage {
    private long timestamp;
    private int timestampDelta;

    private byte[] textData;

    public TextMessage(long timestamp, int timestampDelta, byte[] textData) {
        super(10);
        this.timestamp = timestamp;
        this.timestampDelta = timestampDelta;
        this.textData = textData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getTimestampDelta() {
        return timestampDelta;
    }

    public byte[] getTextData() {
        return textData;
    }
}
