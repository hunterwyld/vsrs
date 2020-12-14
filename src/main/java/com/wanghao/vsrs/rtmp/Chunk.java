package com.wanghao.vsrs.rtmp;

import io.netty.buffer.ByteBuf;

/**
 * @author wanghao
 */
public class Chunk {
    /** Chunk Basis Header */
    // 2-bit
    private byte fmt;
    // 0~65599 inclusive; 0, 1, and 2 are reserved
    private int csid;

    private ChunkMessageHeader messageHeader;

    // partially decoded payload, actual size <= header.payload_length
    private ByteBuf messagePayload;

    // Decoded msg count, to identify whether the chunk stream is fresh.
    private long messageCount;


    public byte getFmt() {
        return fmt;
    }

    public void setFmt(byte fmt) {
        this.fmt = fmt;
    }

    public int getCsid() {
        return csid;
    }

    public void setCsid(int csid) {
        this.csid = csid;
    }

    public ChunkMessageHeader getMessageHeader() {
        return messageHeader;
    }

    public void setMessageHeader(ChunkMessageHeader messageHeader) {
        this.messageHeader = messageHeader;
    }

    public ByteBuf getMessagePayload() {
        return messagePayload;
    }

    public void setMessagePayload(ByteBuf messagePayload) {
        this.messagePayload = messagePayload;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }
}
