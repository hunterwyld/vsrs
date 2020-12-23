package com.wanghao.vsrs.common.handler;

import com.wanghao.vsrs.common.rtmp.message.*;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author wanghao
 */
public interface MessageHandler {

    void handleVideo(VideoMessage msg);

    void handleAudio(AudioMessage msg);

    void handleText(TextMessage msg);

    void handleAMF0Data(ChannelHandlerContext ctx, AMF0DataMessage data);

    void handleAMF0Command(ChannelHandlerContext ctx, AMF0CommandMessage command);
}
