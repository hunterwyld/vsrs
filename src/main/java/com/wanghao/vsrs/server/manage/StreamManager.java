package com.wanghao.vsrs.server.manage;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wanghao
 */
public class StreamManager {
    private static final Logger logger = Logger.getLogger(StreamManager.class);

    private static final ScheduledExecutorService timer;
    static {
        timer = new ScheduledThreadPoolExecutor(1);
        timer.scheduleWithFixedDelay(new StreamCleaner(), 30, 30, TimeUnit.SECONDS);
    }

    // map: StreamId -> Stream
    private static final ConcurrentHashMap<StreamId, Stream> streamMap = new ConcurrentHashMap<>();

    public static boolean createStream(StreamId streamId, Stream stream) {
        if (streamMap.containsKey(streamId)) {
            return false;
        }
        streamMap.put(streamId, stream);
        return true;
    }

    public static Stream getStream(StreamId streamId) {
        return streamMap.get(streamId);
    }

    public static void deleteStream(StreamId streamId) {
        streamMap.remove(streamId);
    }


    private static class StreamCleaner implements Runnable {
        @Override
        public void run() {
            for (Map.Entry<StreamId, Stream> entry : streamMap.entrySet()) {
                Stream stream = entry.getValue();
                if (stream == null || stream.getPublisher() == null || !stream.getPublisher().isActive()) {
                    logger.info("clean stream=" + entry.getKey());
                    deleteStream(entry.getKey());
                }
            }
        }
    }
}
