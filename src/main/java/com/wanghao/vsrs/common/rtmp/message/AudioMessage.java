package com.wanghao.vsrs.common.rtmp.message;

/**
 * @author wanghao
 */
public class AudioMessage extends RtmpMessage {
    private long timestamp;
    private int timestampDelta;

    private int control;

    private byte[] audioData;

    public AudioMessage(long timestamp, int timestampDelta, int control, byte[] audioData) {
        super(8);
        this.timestamp = timestamp;
        this.timestampDelta = timestampDelta;
        this.control = control;
        this.audioData = audioData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getTimestampDelta() {
        return timestampDelta;
    }

    public int getControl() {
        return control;
    }

    public byte[] getAudioData() {
        return audioData;
    }
}
