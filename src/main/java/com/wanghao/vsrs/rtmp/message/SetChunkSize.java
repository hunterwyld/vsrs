package com.wanghao.vsrs.rtmp.message;

/**
 * @author wanghao
 */
public class SetChunkSize extends ProtocolControlMessage {
    private int chunkSize;

    public SetChunkSize(int chunkSize) {
        super(1);
        this.chunkSize = chunkSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }
}


