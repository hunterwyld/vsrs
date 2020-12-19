package com.wanghao.vsrs.common.rtmp.message;

/**
 * @author wanghao
 */
public class RtmpMessage {
    private int messageTypeId;

    public RtmpMessage(int messageTypeId) {
        this.messageTypeId = messageTypeId;
    }

    public int getMessageTypeId() {
        return messageTypeId;
    }
}
