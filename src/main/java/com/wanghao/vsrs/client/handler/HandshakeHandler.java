package com.wanghao.vsrs.client.handler;

import com.wanghao.vsrs.common.rtmp.HandshakeBytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.log4j.Logger;

import java.util.List;

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
        }
    }
}
