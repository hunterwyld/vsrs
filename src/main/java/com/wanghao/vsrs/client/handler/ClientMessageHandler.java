package com.wanghao.vsrs.client.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wanghao.vsrs.common.handler.MessageHandler;
import com.wanghao.vsrs.common.rtmp.message.*;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0Object;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.wanghao.vsrs.client.conf.Properties.isPlayer;
import static com.wanghao.vsrs.client.conf.Properties.publishFilePath;
import static com.wanghao.vsrs.common.util.Constant.VIDEO_CONTROL_KEYFRAME;

/**
 * @author wanghao
 */
public class ClientMessageHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(ClientMessageHandler.class);

    private boolean readyToPlay = false;

    @Override
    public void handleVideo(VideoMessage msg) {
        if (isPlayer && readyToPlay) {
            byte[] videoData = msg.getVideoData();
            logger.info("<-- " + new String(videoData, 1, videoData.length-1));
        }
    }

    @Override
    public void handleAudio(AudioMessage msg) {

    }

    @Override
    public void handleText(TextMessage msg) {
        if (isPlayer && readyToPlay) {
            logger.info("<-- " + new String(msg.getTextData()));
        }
    }

    @Override
    public void handleAMF0Data(ChannelHandlerContext ctx, AMF0DataMessage data) {

    }

    @Override
    public void handleAMF0Command(ChannelHandlerContext ctx, AMF0CommandMessage command) {
        if (command == null) {
            return;
        }
        List<Object> decodedObjectList = command.getObjectList();
        if (decodedObjectList == null) {
            return;
        }
        String commandName = null;
        for (Object decodedObj : decodedObjectList) {
            int type = AMF0.getType(decodedObj);
            if (type == AMF0.String) {
                commandName = (String) decodedObj;
                break;
            }
        }
        if (commandName == null) {
            return;
        }

        doHandleAMF0Command(ctx, commandName, command);
    }

    private void doHandleAMF0Command(ChannelHandlerContext ctx, final String commandName,final AMF0CommandMessage command) {
        logger.info("<-- recv command: " + commandName);
        switch (commandName) {
            case "onStatus":
                doHandleOnStatus(ctx, command);
                return;
            default:
                return;
        }
    }

    private void doHandleOnStatus(ChannelHandlerContext ctx, AMF0CommandMessage command) {
        for (Object obj : command.getObjectList()) {
            if (!(obj instanceof AMF0Object)) {
                continue;
            }
            if (isPlayer && "NetStream.Play.Start".equals(((AMF0Object) obj).get("code"))) {
                readyToPlay = true;
                break;
            }
            if (!isPlayer && "NetStream.Publish.Start".equals(((AMF0Object) obj).get("code"))) {
                startPublishText(ctx);
                break;
            }
        }
    }

    private void startPublishText(ChannelHandlerContext ctx) {
        String textToPublish = readPublishFile();
        if (textToPublish == null) {
            logger.error("read file error: " + publishFilePath);
            return;
        }
        //String textToPublish = "[{\"time\":1608711651000,\"data\":{\"user\":\"我是丁真\",\"text\":\"我要去喂我的小马了\"}},{\"time\":1608711652000,\"data\":{\"user\":\"马保国\",\"text\":\"年轻人不讲武德\"}},{\"time\":1608711654000,\"data\":{\"user\":\"cxk\",\"text\":\"吃我一记连五鞭\"}},{\"time\":1608711654500,\"data\":{\"user\":\"我是丁真\",\"text\":\"cxk是我大哥\"}},{\"time\":1608711654800,\"data\":{\"user\":\"马保国\",\"text\":\"就骗！就偷袭！\"}},{\"time\":1608711655500,\"data\":{\"user\":\"cxk\",\"text\":\"心疼保国老师...\"}},{\"time\":1608711655600,\"data\":{\"user\":\"我是丁真\",\"text\":\"传统功夫讲究点到为止\"}},{\"time\":1608711655900,\"data\":{\"user\":\"马保国\",\"text\":\"噫！大意了，没有闪\"}},{\"time\":1608711656000,\"data\":{\"user\":\"cxk\",\"text\":\"我劝你们年轻人耗子尾汁\"}},{\"time\":1608711656050,\"data\":{\"user\":\"马保国\",\"text\":\"浑元形意太极门掌门人\"}}]";
        final Object[] objects = JSON.parseArray(textToPublish).toArray();
        logger.info("Ready to publish " + objects.length + " messages");
        int curIdx = 0;
        long beginTime = System.currentTimeMillis();
        long gapTime = Long.MIN_VALUE;
        while (curIdx < objects.length) {
            Object curObj = objects[curIdx];
            Long curTime = ((JSONObject)curObj).getLong("time");
            if (curIdx == 0) {
                gapTime = System.currentTimeMillis() - curTime;
            } else {
                long now = System.currentTimeMillis();
                while (now < gapTime + curTime) {
                    //wait
                    now = System.currentTimeMillis();
                }
            }
            JSONObject data = ((JSONObject) curObj).getJSONObject("data");
            String message = data.getString("user") + ": " + data.getString("text");
            logger.info("--> " + message);
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] videoData = new byte[1+bytes.length];
            videoData[0] = 0x01;
            System.arraycopy(bytes, 0, videoData, 1, bytes.length);
            VideoMessage msg = new VideoMessage(System.currentTimeMillis()-beginTime, 0, VIDEO_CONTROL_KEYFRAME, videoData);
            //TextMessage msg = new TextMessage(System.currentTimeMillis()-beginTime, 0, bytes);
            curIdx++;
            ctx.channel().writeAndFlush(msg);
        }
    }

    private String readPublishFile() {
        if (publishFilePath == null) {
            logger.error("no publish file specified");
            return null;
        }

        InputStream in;
        try {
            in = new FileInputStream(publishFilePath);
        } catch (FileNotFoundException e) {
            logger.error("publish file not found", e);
            return null;
        }

        BufferedReader reader = null;
        try {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                line = reader.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
