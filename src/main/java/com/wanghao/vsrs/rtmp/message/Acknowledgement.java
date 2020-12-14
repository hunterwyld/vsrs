package com.wanghao.vsrs.rtmp.message;

/**
 * @author wanghao
 */
public class Acknowledgement extends ProtocolControlMessage {
    private int sequenceNumber;

    public Acknowledgement(int sequenceNumber) {
        super(3);
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
}
