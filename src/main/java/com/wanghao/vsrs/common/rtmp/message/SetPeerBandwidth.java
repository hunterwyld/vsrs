package com.wanghao.vsrs.common.rtmp.message;

/**
 * @author wanghao
 */
public class SetPeerBandwidth extends ProtocolControlMessage {
    private int ackSize;

    private byte limitType;

    public SetPeerBandwidth(int ackSize, byte limitType) {
        super(6);
        this.ackSize = ackSize;
        this.limitType = limitType;
    }

    public int getAckSize() {
        return ackSize;
    }

    public byte getLimitType() {
        return limitType;
    }
}
