package com.wanghao.vsrs.common.rtmp.message;

import java.util.List;

/**
 * @author wanghao
 */
public class AMF0CommandMessage extends RtmpMessage {
    private int csid;

    private List<Object> objectList;

    public AMF0CommandMessage(int csid, List<Object> objectList) {
        super(20);
        this.csid = csid;
        this.objectList = objectList;
    }

    public int getCsid() {
        return csid;
    }

    public List<Object> getObjectList() {
        return objectList;
    }
}
