package com.wanghao.vsrs.common.rtmp.message;

import java.util.List;

/**
 * @author wanghao
 */
public class AMF0DataMessage extends RtmpMessage {
    private int csid;

    private List<Object> dataList;

    public AMF0DataMessage(int csid, List<Object> dataList) {
        super(18);
        this.csid = csid;
        this.dataList = dataList;
    }

    public int getCsid() {
        return csid;
    }

    public List<Object> getDataList() {
        return dataList;
    }
}
