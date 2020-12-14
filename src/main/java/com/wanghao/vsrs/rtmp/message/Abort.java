package com.wanghao.vsrs.rtmp.message;

/**
 * @author wanghao
 */
public class Abort extends ProtocolControlMessage {
    private int toAbortCsid;

    public Abort(int toAbortCsid) {
        super(2);
        this.toAbortCsid = toAbortCsid;
    }

    public int getToAbortCsid() {
        return toAbortCsid;
    }
}
