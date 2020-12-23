package com.wanghao.vsrs.client.handler.netty;

import com.wanghao.vsrs.common.rtmp.HandshakeBytes;
import com.wanghao.vsrs.common.rtmp.message.AMF0CommandMessage;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0Object;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.wanghao.vsrs.client.conf.Properties.*;

/**
 * @author wanghao
 */
public class HandshakeHandler extends ByteToMessageDecoder {
    private static final Logger logger = Logger.getLogger(HandshakeHandler.class);

    private boolean handshakeDone = false;

    private HandshakeBytes handshakeBytes;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (handshakeBytes == null) {
            handshakeBytes = new HandshakeBytes();
        }

        handshakeBytes.write_c0c1(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (handshakeDone) {
            ctx.fireChannelRead(in);
            return;
        }

        // TODO: complex handshake

        // simple handshake
        boolean s0s1s2Read = handshakeBytes.read_s0s1s2(in);
        if (s0s1s2Read) {
            handshakeBytes.write_c2(ctx);
            handshakeDone = true;
            // 握手成功，从pipeline中移除
            ctx.channel().pipeline().remove(this);
            logger.info("handshake success from channel: " + ctx.channel().id());

            onHandshakeSuccess(ctx);
        }
    }

    private void onHandshakeSuccess(ChannelHandlerContext ctx) {
        final int outCsid = 3;

        // send connect('live')
        logger.info("--> send connect('live')");
        List<Object> connect = new ArrayList<>();
        connect.add("connect");
        connect.add(1);
        AMF0Object amf0Object = new AMF0Object();
        amf0Object.put("app", appName);
        connect.add(amf0Object);
        AMF0CommandMessage connectCommand = new AMF0CommandMessage(outCsid, connect);
        ctx.channel().writeAndFlush(connectCommand);

        if (isPlayer) {
            // send createStream()
            logger.info("--> send createStream()");
            List<Object> createStream = new ArrayList<>();
            createStream.add("createStream");
            createStream.add(2);
            createStream.add(null);
            AMF0CommandMessage createStreamCommand = new AMF0CommandMessage(outCsid, createStream);
            ctx.channel().writeAndFlush(createStreamCommand);

            // send play
            logger.info("--> send play");
            List<Object> play = new ArrayList<>();
            play.add("play");
            play.add(3);
            play.add(null);
            play.add(streamName);
            play.add(0);
            AMF0CommandMessage playCommand = new AMF0CommandMessage(outCsid, play);
            ctx.channel().writeAndFlush(playCommand);
        } else {
            // send releaseStream()
            logger.info("--> send releaseStream()");
            List<Object> releaseStream = new ArrayList<>();
            releaseStream.add("releaseStream");
            releaseStream.add(2);
            releaseStream.add(null);
            releaseStream.add(streamName);
            AMF0CommandMessage releaseStreamCommand = new AMF0CommandMessage(outCsid, releaseStream);
            ctx.channel().writeAndFlush(releaseStreamCommand);

            // send FCPublish
            // send createStream()
            logger.info("--> send FCPublish");
            List<Object> FCPublish = new ArrayList<>();
            FCPublish.add("FCPublish");
            FCPublish.add(3);
            FCPublish.add(null);
            FCPublish.add(streamName);
            AMF0CommandMessage FCPublishCommand = new AMF0CommandMessage(outCsid, FCPublish);
            ctx.channel().writeAndFlush(FCPublishCommand);

            // send createStream()
            logger.info("--> send createStream()");
            List<Object> createStream = new ArrayList<>();
            createStream.add("createStream");
            createStream.add(2);
            createStream.add(null);
            AMF0CommandMessage createStreamCommand = new AMF0CommandMessage(outCsid, createStream);
            ctx.channel().writeAndFlush(createStreamCommand);

            // send publish
            logger.info("--> send publish");
            List<Object> publish = new ArrayList<>();
            publish.add("publish");
            publish.add(6);
            publish.add(null);
            publish.add(streamName);
            publish.add(appName);
            AMF0CommandMessage publishCommand = new AMF0CommandMessage(outCsid, publish);
            ctx.channel().writeAndFlush(publishCommand);
        }
    }
}
