package com.wanghao.vsrs.rtmp.message;

import static com.wanghao.vsrs.util.Constant.VIDEO_CONTROL_KEYFRAME;

/**
 * @author wanghao
 */
public class VideoMessage extends RtmpMessage {
    private long timestamp;
    private int timestampDelta;

    private int control;

    private byte[] videoData;

    public VideoMessage(long timestamp, int timestampDelta, int control, byte[] videoData) {
        super(9);
        this.timestamp = timestamp;
        this.timestampDelta = timestampDelta;
        this.control = control;
        this.videoData = videoData;
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

    public byte[] getVideoData() {
        return videoData;
    }

    public boolean isH264KeyFrame() {
        return control == VIDEO_CONTROL_KEYFRAME;
    }

    public boolean isAVCDecoderConfigurationRecord() {
        return isH264KeyFrame() && videoData.length > 1 && videoData[0] == 0x00;
    }
}
