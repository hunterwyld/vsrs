package com.wanghao.vsrs.server.handler;

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

    private boolean c0c1Done = false;
    private boolean handshakeDone = false;

    private HandshakeBytes handshakeBytes;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (handshakeDone) {
            ctx.fireChannelRead(in);
            return;
        }

        if (handshakeBytes == null) {
            handshakeBytes = new HandshakeBytes();
        }

        // TODO: complex handshake


        // simple handshake
        if (!c0c1Done) {
            handshakeBytes.read_c0c1(in);
            handshakeBytes.write_s0s1s2(ctx);
            c0c1Done = true;
        } else {
            handshakeBytes.read_c2(in);
            handshakeDone = true;
            // 握手成功，从pipeline中移除
            ctx.channel().pipeline().remove(this);
            logger.info("handshake success from channel: " + ctx.channel().id());
        }
    }
}
