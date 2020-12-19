package com.wanghao.vsrs.common.rtmp;

import com.wanghao.vsrs.common.err.ProtocolException;
import com.wanghao.vsrs.common.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author wanghao
 */
public class HandshakeBytes {
    /** 1+1536 bytes */
    private ByteBuf c0c1;

    /** 1+1536+1536 bytes */
    private ByteBuf s0s1s2;

    /** 1536 bytes */
    private ByteBuf c2;

    public void read_c0c1(ByteBuf in) {
        if (c0c1 != null) {
            return;
        }
        c0c1 = Unpooled.buffer(1537);
        in.readBytes(c0c1);
        if (c0c1.array()[0] != 0x03) {
            throw new ProtocolException("read c0c1: only support version 3");
        }
    }

    public void write_c0c1(ChannelHandlerContext ctx) {
        if (c0c1 == null) {
            c0c1 = Unpooled.buffer(1537);
        } else {
            c0c1.clear();
        }

        // c0: rtmp version 3
        c0c1.writeByte(0x03);
        // c1 time: 4-byte timestamp
        c0c1.writeInt((int) (System.currentTimeMillis()/1000));
        // c1 zero: 4-byte zero
        c0c1.writeInt(0);
        // c1 random: 1536-8=1528 bytes
        c0c1.writeBytes(Utils.generateRandomBytes(1528));

        ctx.writeAndFlush(c0c1);
    }

    public boolean read_s0s1s2(ByteBuf in) {
        if (s0s1s2 != null) {
            return true;
        }

        if (in.readableBytes() >= 3073) {
            s0s1s2 = Unpooled.buffer(3073);
            in.readBytes(s0s1s2);
            return true;
        }
        return false;
    }

    public void write_s0s1s2(ChannelHandlerContext ctx) {
        if (s0s1s2 == null) {
            s0s1s2 = Unpooled.buffer(3073);
        } else {
            s0s1s2.clear();
        }

        // s0: rtmp version 3
        s0s1s2.writeByte(0x03);
        // s1 time: 4-byte timestamp
        s0s1s2.writeInt((int) (System.currentTimeMillis()/1000));
        // s1 zero: 4-byte zero
        s0s1s2.writeInt(0);
        // s1 random: 1536-8=1528 bytes
        s0s1s2.writeBytes(Utils.generateRandomBytes(1528));
        // s2 copy from c1
        if (c0c1 != null) {
            s0s1s2.writeBytes(c0c1, 1, 1536);
        }

        ctx.writeAndFlush(s0s1s2);
    }

    public void read_c2(ByteBuf in) {
        if (c2 != null) {
            return;
        }
        c2 = Unpooled.buffer(1536);
        in.readBytes(c2);
    }

    public void write_c2(ChannelHandlerContext ctx) {
        if (c2 == null) {
            c2 = Unpooled.buffer(1536);
        } else {
            c2.clear();
        }
        // c2 copy from s1
        if (s0s1s2 != null) {
            c2.writeBytes(s0s1s2, 1, 1536);
            ctx.writeAndFlush(c2);
        }
    }


}
