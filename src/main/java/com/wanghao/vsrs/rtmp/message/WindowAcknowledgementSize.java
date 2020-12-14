package com.wanghao.vsrs.rtmp.message;

/**
 * @author wanghao
 */
public class WindowAcknowledgementSize extends ProtocolControlMessage {
    private int ackSize;
    public WindowAcknowledgementSize(int ackSize) {
        super(5);
        this.ackSize = ackSize;
    }

    public int getAckSize() {
        return ackSize;
    }
}
