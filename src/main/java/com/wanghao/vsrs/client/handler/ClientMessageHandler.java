package com.wanghao.vsrs.client.handler;

import com.wanghao.vsrs.common.handler.MessageHandler;
import com.wanghao.vsrs.common.rtmp.message.*;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0Object;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;

import java.util.List;

import static com.wanghao.vsrs.client.conf.Properties.isPlayer;

/**
 * @author wanghao
 */
public class ClientMessageHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(ClientMessageHandler.class);

    private boolean readyToPlay = false;
    private boolean readyToPublish = false;

    @Override
    public void handleVideo(VideoMessage msg) {

    }

    @Override
    public void handleAudio(AudioMessage msg) {

    }

    @Override
    public void handleText(TextMessage msg) {
        if (!readyToPlay) {
            return;
        }
        logger.info(new String(msg.getTextData()));
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
                readyToPublish = true;
                break;
            }
        }
    }
}
