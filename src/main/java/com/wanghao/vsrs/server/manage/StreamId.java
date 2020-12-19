package com.wanghao.vsrs.server.manage;

/**
 * @author wanghao
 */
public class StreamId {
    private String appName;
    private String streamName;

    public StreamId(String appName, String streamName) {
        this.appName = appName;
        this.streamName = streamName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamId streamId = (StreamId) o;

        if (!appName.equals(streamId.appName)) return false;
        return streamName.equals(streamId.streamName);
    }

    @Override
    public int hashCode() {
        int result = appName.hashCode();
        result = 31 * result + streamName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "StreamId{" +
                "appName='" + appName + '\'' +
                ", streamName='" + streamName + '\'' +
                '}';
    }
}
