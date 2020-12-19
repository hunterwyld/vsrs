package com.wanghao.vsrs.server.handler;

import com.wanghao.vsrs.common.rtmp.message.*;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.log4j.Logger;

import static com.wanghao.vsrs.common.util.Constant.*;

/**
 * @author wanghao
 */
public class ChunkEncoder extends MessageToByteEncoder<RtmpMessage> {
    private static final Logger logger = Logger.getLogger(ChunkEncoder.class);

    // The output chunk size, default to min, set by peer
    private int outChunkSize = RTMP_MIN_CHUNK_SIZE;

    private boolean firstVideo = true;
    private boolean firstAudio = true;
    private long firstVideoTimestamp = System.currentTimeMillis();
    private long firstAudioTimestamp = System.currentTimeMillis();

    @Override
    protected void encode(ChannelHandlerContext ctx, RtmpMessage msg, ByteBuf out) throws Exception {
        if (msg instanceof SetChunkSize) {
            SetChunkSize setChunkSize = (SetChunkSize) msg;
            // first byte: fmt + csid
            out.writeByte(setChunkSize.getCsid());
            // timestamp, message length, message type id, message stream id
            out.writeMedium(0).writeMedium(4).writeByte(setChunkSize.getMessageTypeId()).writeInt(setChunkSize.getMessageStreamId());
            // body
            out.writeInt(setChunkSize.getChunkSize());
            outChunkSize = setChunkSize.getChunkSize();
        } else if (msg instanceof WindowAcknowledgementSize) {
            WindowAcknowledgementSize was = (WindowAcknowledgementSize) msg;
            // first byte: fmt + csid
            out.writeByte(was.getCsid());
            // timestamp, message length, message type id, message stream id
            out.writeMedium(0).writeMedium(4).writeByte(was.getMessageTypeId()).writeInt(was.getMessageStreamId());
            // body
            out.writeInt(was.getAckSize());
        } else if (msg instanceof SetPeerBandwidth) {
            SetPeerBandwidth setPeerBandwidth = (SetPeerBandwidth) msg;
            // first byte: fmt + csid
            out.writeByte(setPeerBandwidth.getCsid());
            // timestamp, message length, message type id, message stream id
            out.writeMedium(0).writeMedium(5).writeByte(setPeerBandwidth.getMessageTypeId()).writeInt(setPeerBandwidth.getMessageStreamId());
            // body
            out.writeInt(setPeerBandwidth.getAckSize()).writeByte(setPeerBandwidth.getLimitType());
        } else if (msg instanceof UserControlMessage) {
            UserControlMessage ucm = (UserControlMessage) msg;
            // first byte: fmt + csid
            out.writeByte(ucm.getCsid());
            // timestamp, message length, message type id, message stream id
            out.writeMedium(0).writeMedium(6).writeByte(ucm.getMessageTypeId()).writeInt(ucm.getMessageStreamId());
            // body
            out.writeShort(ucm.getEventType()).writeInt(ucm.getData());
        } else if (msg instanceof AMF0CommandMessage) {
            AMF0CommandMessage toBeEncoded = (AMF0CommandMessage) msg;

            // encode amf0 body to a tmp output buffer first, aiming to get the payload length
            ByteBuf tmpOut = AMF0.encodeAll(toBeEncoded.getObjectList());
            if (tmpOut != null) {
                // first byte: fmt + csid
                out.writeByte(toBeEncoded.getCsid());
                // timestamp, message length, message type id, message stream id
                out.writeMedium(0).writeMedium(tmpOut.writerIndex()).writeByte(toBeEncoded.getMessageTypeId()).writeInt(0);
                // body
                out.writeBytes(tmpOut);
            }
        } else if (msg instanceof AMF0DataMessage) {
            AMF0DataMessage toBeEncoded = (AMF0DataMessage) msg;

            // encode amf0 body to a tmp output buffer first, aiming to get the payload length
            ByteBuf tmpOut = AMF0.encodeAll(toBeEncoded.getDataList());
            if (tmpOut != null) {
                // first byte: fmt + csid
                out.writeByte(toBeEncoded.getCsid());
                // timestamp, message length, message type id, message stream id
                out.writeMedium(0).writeMedium(tmpOut.writerIndex()).writeByte(toBeEncoded.getMessageTypeId()).writeInt(0);
                // body
                out.writeBytes(tmpOut);
            }
        } else if (msg instanceof VideoMessage) {
            if (firstVideo) {
                firstVideoTimestamp = System.currentTimeMillis();
                encodeWithFmt0And3(msg, out);
                firstVideo = false;
            } else {
                encodeWithFmt1And3(msg, out);
            }
        } else if (msg instanceof AudioMessage) {
            if (firstAudio) {
                firstAudioTimestamp = System.currentTimeMillis();
                encodeWithFmt0And3(msg, out);
                firstAudio = false;
            } else {
                encodeWithFmt1And3(msg, out);
            }
        }
    }

    private void encodeWithFmt0And3(final RtmpMessage msg, final ByteBuf out) {
        int outCsid;
        int payloadLength;
        ByteBuf payload;
        long firstMediaTimestamp;

        if (msg instanceof AudioMessage) {
            outCsid = DEFAULT_AUDIO_OUT_CSID;
            payloadLength = 1 + ((AudioMessage) msg).getAudioData().length; // control + audio data
            payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
            payload.writeByte(((AudioMessage) msg).getControl());
            payload.writeBytes(((AudioMessage) msg).getAudioData());
            firstMediaTimestamp = firstAudioTimestamp;
        } else if (msg instanceof VideoMessage) {
            outCsid = DEFAULT_VIDEO_OUT_CSID;
            payloadLength = 1 + ((VideoMessage) msg).getVideoData().length; // control + video data
            payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
            payload.writeByte(((VideoMessage) msg).getControl());
            payload.writeBytes(((VideoMessage) msg).getVideoData());
            firstMediaTimestamp = firstVideoTimestamp;
        } else {
            return;
        }

        ByteBuf tmpOut = Unpooled.buffer();
        byte[] basicHeader = encodeBasicHeader(0, outCsid);
        tmpOut.writeBytes(basicHeader); // basic header
        boolean needExtendedTimestamp = false;
        long timestamp = System.currentTimeMillis() - firstMediaTimestamp;
        if (timestamp >= RTMP_EXTENDED_TIMESTAMP) { // timestamp
            needExtendedTimestamp = true;
            tmpOut.writeMedium(RTMP_EXTENDED_TIMESTAMP);
        } else {
            tmpOut.writeMedium((int) timestamp);
        }
        tmpOut.writeMedium(payloadLength); // message length
        tmpOut.writeByte(msg.getMessageTypeId()); // message type id
        tmpOut.writeIntLE(0); // stream id default to 0
        if (needExtendedTimestamp) { // extended timestamp if necessary
            tmpOut.writeInt((int) timestamp);
        }

        if (payloadLength <= outChunkSize) {
            tmpOut.writeBytes(payload);
            out.writeBytes(tmpOut);
            return;
        }

        tmpOut.writeBytes(payload, outChunkSize);
        while (payload.isReadable()) {
            int remainSize = payload.readableBytes();
            int chunkSize = Math.min(outChunkSize, remainSize);
            byte[] fm3BasicHeader = encodeBasicHeader(3, outCsid);
            tmpOut.writeBytes(fm3BasicHeader);
            tmpOut.writeBytes(payload, chunkSize);
        }
        out.writeBytes(tmpOut);
    }

    private void encodeWithFmt1And3(final RtmpMessage msg, final ByteBuf out) {
        int outCsid;
        int payloadLength;
        ByteBuf payload;
        int timestampDelta;

        if (msg instanceof AudioMessage) {
            outCsid = DEFAULT_AUDIO_OUT_CSID;
            payloadLength = 1 + ((AudioMessage) msg).getAudioData().length; // control + audio data
            payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
            payload.writeByte(((AudioMessage) msg).getControl());
            payload.writeBytes(((AudioMessage) msg).getAudioData());
            timestampDelta = ((AudioMessage) msg).getTimestampDelta();
        } else if (msg instanceof VideoMessage) {
            outCsid = DEFAULT_VIDEO_OUT_CSID;
            payloadLength = 1 + ((VideoMessage) msg).getVideoData().length; // control + video data
            payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
            payload.writeByte(((VideoMessage) msg).getControl());
            payload.writeBytes(((VideoMessage) msg).getVideoData());
            timestampDelta = ((VideoMessage) msg).getTimestampDelta();
        } else {
            return;
        }

        ByteBuf tmpOut = Unpooled.buffer();
        byte[] basicHeader = encodeBasicHeader(1, outCsid);
        tmpOut.writeBytes(basicHeader); // basic header
        tmpOut.writeMedium(timestampDelta); // timestamp delta
        tmpOut.writeMedium(payloadLength); // message length
        tmpOut.writeByte(msg.getMessageTypeId()); // message type id

        if (payloadLength <= outChunkSize) {
            tmpOut.writeBytes(payload);
            out.writeBytes(tmpOut);
            return;
        }

        tmpOut.writeBytes(payload, outChunkSize);
        while (payload.isReadable()) {
            int remainSize = payload.readableBytes();
            int chunkSize = Math.min(outChunkSize, remainSize);
            byte[] fm3BasicHeader = encodeBasicHeader(3, outCsid);
            tmpOut.writeBytes(fm3BasicHeader);
            tmpOut.writeBytes(payload, chunkSize);
        }
        out.writeBytes(tmpOut);
    }

    private static byte[] encodeBasicHeader(final int fmt, final int csid) {
        if (csid <= 63) {
            return new byte[] { (byte) ((fmt << 6) + csid) };
        } else if (csid <= 320) {
            return new byte[] { (byte) (fmt << 6), (byte) (csid - 64) };
        } else {
            return new byte[] { (byte) ((fmt << 6) | 1), (byte) ((csid - 64) & 0xff), (byte) ((csid - 64) >> 8) };
        }
    }
}
