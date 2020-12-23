package com.wanghao.vsrs.common.handler.netty;

import com.wanghao.vsrs.client.handler.ClientMessageHandler;
import com.wanghao.vsrs.common.err.ProtocolException;
import com.wanghao.vsrs.common.handler.MessageHandler;
import com.wanghao.vsrs.common.rtmp.Chunk;
import com.wanghao.vsrs.common.rtmp.ChunkMessageHeader;
import com.wanghao.vsrs.server.handler.ServerMessageHandler;
import com.wanghao.vsrs.common.rtmp.message.*;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wanghao.vsrs.common.util.Constant.*;

/**
 * @author wanghao
 */
public class ChunkDecoder extends ReplayingDecoder<ChunkDecoder.DecodeState> {
    private static final Logger logger = Logger.getLogger(ChunkDecoder.class);

    // The input chunk size, default to min, set by peer
    private int inChunkSize = RTMP_MIN_CHUNK_SIZE;

    private byte curFmt = -1;
    private int curCsid = -1;

    // store the last decoded chunk of specified csid
    private final Map<Integer, Chunk> csid2ChunkMap = new HashMap<>();

    private final MessageHandler messageHandler;
    private final boolean isServer;
    private final boolean willMockText = false;

    public ChunkDecoder(boolean isServer) {
        super();
        this.isServer = isServer;
        if (isServer) {
            messageHandler = new ServerMessageHandler();
        } else {
            messageHandler = new ClientMessageHandler();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // get current decode state
        DecodeState state = state();
        if (state == null) {
            state = DecodeState.READY_TO_DECODE_HEADER;
        }

        if (DecodeState.READY_TO_DECODE_HEADER.equals(state)) {
            readChunkBasicHeader(in);
            readChunkMessageHeader(in);
            checkpoint(DecodeState.READY_TO_DECODE_PAYLOAD);
        } else if (DecodeState.READY_TO_DECODE_PAYLOAD.equals(state)){
            Chunk chunk = csid2ChunkMap.get(curCsid);
            if (chunk == null || chunk.getMessageHeader() == null) {
                throw new RuntimeException("inner error: chunk or chunk message header should not be null");
            }
            int payloadLength = chunk.getMessageHeader().getPayloadLength();
            if (payloadLength <= 0) {
                logger.info("ignore a message with no payload");
                checkpoint(DecodeState.READY_TO_DECODE_HEADER);
                chunk.setMessagePayload(null);
                return;
            }

            ByteBuf payload = chunk.getMessagePayload();
            if (payload == null) {
                // initialize payload if null
                payload = Unpooled.buffer(payloadLength, payloadLength);
                chunk.setMessagePayload(payload);
            }

            int toReadSize = Math.min(payload.writableBytes(), inChunkSize);
            final byte[] bytes = new byte[toReadSize];
            // in.readBytes(payload) is prohibited here, because it is not replayable
            in.readBytes(bytes);
            // write to payload
            payload.writeBytes(bytes);
            checkpoint(DecodeState.READY_TO_DECODE_HEADER);

            if (payload.isWritable()) {
                // payload not complete yet, continue reading payload
                return;
            }

            chunk.setMessagePayload(null);
            // now we got a message with complete payload
            RtmpMessage decodedMessage = onRecvCompleteMessage(ctx, chunk.getMessageHeader(), payload);
            if (decodedMessage == null) {
                logger.info("ignore an uninterested message");
                return;
            }
            // pass to the next inbound handler in channel pipeline, if exist
            out.add(decodedMessage);
        }
    }

    private RtmpMessage onRecvCompleteMessage(ChannelHandlerContext ctx, ChunkMessageHeader header, ByteBuf payload) {
        RtmpMessage retMsg = null;

        byte messageTypeId = header.getMessageTypeId();
        switch (messageTypeId) {
            case RTMP_MSG_SetChunkSize:
                int chunkSize = payload.readInt();
                if (chunkSize > RTMP_MAX_CHUNK_SIZE) {
                    logger.warn("accept large chunk size=" + chunkSize);
                }
                if (chunkSize < RTMP_MIN_CHUNK_SIZE) {
                    throw new ProtocolException("chunk size = "+chunkSize+" smaller than 128");
                }
                inChunkSize = chunkSize;
                retMsg = new SetChunkSize(chunkSize);
                // response with the same chunk size
                //ctx.channel().writeAndFlush(retMsg);
                break;
            case RTMP_MSG_AbortMessage:
                int toAbortCsid = payload.readInt();
                retMsg = new Abort(toAbortCsid);
                break;
            case RTMP_MSG_Acknowledgement:
                int sequenceNumber = payload.readInt();
                retMsg = new Acknowledgement(sequenceNumber);
                break;
            case RTMP_MSG_WindowAcknowledgementSize: {
                int ackSize = payload.readInt();
                retMsg = new WindowAcknowledgementSize(ackSize);
            }
                break;
            case RTMP_MSG_SetPeerBandwidth:
                int ackSize = payload.readInt();
                byte limitType = payload.readByte();
                retMsg = new SetPeerBandwidth(ackSize, limitType);
                break;
            case RTMP_MSG_AMF0CommandMessage:
                List<Object> decodedObjectList = AMF0.decodeAll(payload);
                retMsg = new AMF0CommandMessage(curCsid, decodedObjectList);
                messageHandler.handleAMF0Command(ctx, (AMF0CommandMessage) retMsg);
                break;
            case RTMP_MSG_AMF3CommandMessage:
                //TODO
                break;
            case RTMP_MSG_AMF0DataMessage:
                List<Object> data = AMF0.decodeAll(payload);
                retMsg = new AMF0DataMessage(curCsid, data);
                messageHandler.handleAMF0Data(ctx, (AMF0DataMessage) retMsg);
                break;
            case RTMP_MSG_AMF3DataMessage:
                //TODO
                break;
            case RTMP_MSG_UserControlMessage:
                break;
            case RTMP_MSG_AudioMessage: {
                int control = payload.readUnsignedByte();
                if (control != AUDIO_CONTROL) {
                    logger.info("ignore audio message with control = " + control);
                } else {
                    byte[] bytes = new byte[payload.readableBytes()];
                    payload.readBytes(bytes);
                    retMsg = new AudioMessage(header.getTimestamp(), header.getTimestampDelta(), control, bytes);
                    messageHandler.handleAudio((AudioMessage) retMsg);
                }
            }
            break;
            case RTMP_MSG_VideoMessage: {
                int control = payload.readUnsignedByte();
                if (control != VIDEO_CONTROL_KEYFRAME && control != VIDEO_CONTROL_INTERFRAME) {
                    logger.info("ignore video message with control = " + control);
                } else {
                    byte[] bytes = new byte[payload.readableBytes()];
                    payload.readBytes(bytes);
                    if (willMockText && isServer) {
                        String mockStr = "Kratos is Atreus' father!";
                        retMsg = new TextMessage(header.getTimestamp(), header.getTimestampDelta(), mockStr.getBytes(StandardCharsets.UTF_8));
                        messageHandler.handleText((TextMessage) retMsg);
                    } else {
                        retMsg = new VideoMessage(header.getTimestamp(), header.getTimestampDelta(), control, bytes);
                        messageHandler.handleVideo((VideoMessage) retMsg);
                    }
                }
            }
            break;
            case SELFDEFINE_MSG_TextMessage: {
                //for client only
                if (!isServer) {
                    byte[] bytes = new byte[payload.readableBytes()];
                    payload.readBytes(bytes);
                    retMsg = new TextMessage(header.getTimestamp(), header.getTimestampDelta(), bytes);
                    messageHandler.handleText((TextMessage) retMsg);
                }
            }
            break;
            default:
                logger.info("ignore message with type id = " + messageTypeId);
                break;
        }

        return retMsg;
    }

    /** Chunk Basic Header */
    private void readChunkBasicHeader(ByteBuf in) {
        byte firstByte = in.readByte();
        byte fmt = (byte) ((firstByte & 0xff) >> 6);
        int csid = firstByte & 0x3f;
        if (csid == 0) {
            // Basic Header: 2-byte form
            csid = 64 + in.readByte() & 0xff;
        } else if (csid == 1) {
            // Basic Header: 3-byte form
            csid = 64;
            csid += in.readByte() & 0xff;
            csid += (in.readByte() & 0xff) * 256;
        } else {
            // Basic Header: 1-byte form
        }

        curFmt = fmt;
        curCsid = csid;

        if (curFmt == -1 || curCsid == -1) {
            throw new ProtocolException("chunk basic header invalid");
        }
    }

    /** Chunk Message Header */
    private void readChunkMessageHeader(ByteBuf in) {
        byte fmt = curFmt;
        int csid = curCsid;

        Chunk chunk = csid2ChunkMap.get(csid);
        if (chunk == null) {
            chunk = new Chunk();
            csid2ChunkMap.put(csid, chunk);
        }
        chunk.setFmt(fmt);
        chunk.setCsid(csid);

        // fresh packet used to update the timestamp even fmt=3 for first packet.
        // fresh packet always means the chunk is the first one of message.
        boolean isFirstChunkOfMsg = (chunk.getMessagePayload() == null);

        switch (fmt) {
            case FMT_0: {
                if (!isFirstChunkOfMsg) {
                    throw new ProtocolException("for existed chunk, fmt should not be 0");
                }
                int timestampDelta = in.readMedium();
                int payloadLength = in.readMedium();
                byte messageTypeId = (byte) (in.readByte() & 0xff);
                int messageStreamId = in.readIntLE();
                ChunkMessageHeader messageHeader = chunk.getMessageHeader();
                if (messageHeader == null) {
                    messageHeader = new ChunkMessageHeader();
                }
                messageHeader.setPayloadLength(payloadLength);
                messageHeader.setMessageTypeId(messageTypeId);
                messageHeader.setMessageStreamId(messageStreamId);
                messageHeader.setTimestampDelta(timestampDelta);
                if (timestampDelta >= RTMP_EXTENDED_TIMESTAMP) {
                    int extendedTimestamp = in.readInt();
                    messageHeader.setTimestamp(extendedTimestamp);
                } else {
                    messageHeader.setTimestamp(timestampDelta);
                }

                chunk.setMessageHeader(messageHeader);
                // initialize payload
                chunk.setMessagePayload(Unpooled.buffer(payloadLength, payloadLength));
            }
            break;
            case FMT_1: {
                if (chunk.getMessageCount() == 0) {
                    // for librtmp, if ping, it will send a fresh stream with fmt=1
                    logger.warn("fresh chunk starts with fmt=1");
                }
                int timestampDelta = in.readMedium();
                int payloadLength = in.readMedium();
                byte messageTypeId = (byte) (in.readByte() & 0xff);
                ChunkMessageHeader messageHeader = chunk.getMessageHeader();
                if (messageHeader == null) {
                    messageHeader = new ChunkMessageHeader();
                }
                if (isFirstChunkOfMsg) {
                    messageHeader.setPayloadLength(payloadLength);
                } else {
                    if (messageHeader.getPayloadLength() != payloadLength) {
                        throw new ProtocolException("msg in chunk cache, size="+messageHeader.getPayloadLength()+" cannot change to "+payloadLength);
                    }
                }
                messageHeader.setMessageTypeId(messageTypeId);
                messageHeader.setMessageStreamId(messageHeader.getMessageStreamId());
                messageHeader.setTimestampDelta(timestampDelta);
                if (timestampDelta >= RTMP_EXTENDED_TIMESTAMP) {
                    int extendedTimestamp = in.readInt();
                    if (isFirstChunkOfMsg) {
                        messageHeader.setTimestamp(extendedTimestamp);
                    } else {
                        long timestamp = messageHeader.getTimestamp() + extendedTimestamp;
                        messageHeader.setTimestamp(timestamp);
                    }
                } else {
                    long timestamp = messageHeader.getTimestamp() + timestampDelta;
                    messageHeader.setTimestamp(timestamp);
                }

                chunk.setMessageHeader(messageHeader);
                if (isFirstChunkOfMsg) {
                    // initialize payload
                    chunk.setMessagePayload(Unpooled.buffer(payloadLength, payloadLength));
                }
            }
            break;
            case FMT_2: {
                if (chunk.getMessageCount() == 0) {
                    // must be a RTMP protocol level error.
                    throw new ProtocolException("fresh chunk expect fmt=0, actual="+fmt+", csid="+csid);
                }
                int timestampDelta = in.readMedium();
                ChunkMessageHeader messageHeader = chunk.getMessageHeader();
                if (messageHeader == null) {
                    messageHeader = new ChunkMessageHeader();
                }
                messageHeader.setTimestampDelta(timestampDelta);
                if (timestampDelta >= RTMP_EXTENDED_TIMESTAMP) {
                    int extendedTimestamp = in.readInt();
                    long timestamp = messageHeader.getTimestamp() + extendedTimestamp;
                    messageHeader.setTimestamp(timestamp);
                } else {
                    long timestamp = messageHeader.getTimestamp() + timestampDelta;
                    messageHeader.setTimestamp(timestamp);
                }

                chunk.setMessageHeader(messageHeader);

                // do nothing with message payload because it is already initialized
            }
            break;
            case FMT_3: {
                if (chunk.getMessageCount() == 0) {
                    // must be a RTMP protocol level error.
                    throw new ProtocolException("fresh chunk expect fmt=0, actual="+fmt+", csid="+csid);
                }
                // do nothing because chunk message header is already decoded, and message payload is already initialized
            }
            break;
            default:
                throw new ProtocolException("read rtmp header: invalid fmt=" + fmt);
        }

        // increase the msg count
        chunk.setMessageCount(chunk.getMessageCount()+1);
    }


    protected enum DecodeState{
        READY_TO_DECODE_HEADER,
        READY_TO_DECODE_PAYLOAD;
    }
}
