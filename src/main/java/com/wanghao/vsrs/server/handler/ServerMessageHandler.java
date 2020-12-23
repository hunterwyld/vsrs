package com.wanghao.vsrs.server.handler;

import com.wanghao.vsrs.common.handler.MessageHandler;
import com.wanghao.vsrs.server.manage.Stream;
import com.wanghao.vsrs.server.manage.StreamId;
import com.wanghao.vsrs.server.manage.StreamManager;
import com.wanghao.vsrs.common.rtmp.message.*;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0;
import com.wanghao.vsrs.common.rtmp.message.binary.AMF0Object;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wanghao.vsrs.common.util.Constant.APP_NAME;

/**
 * @author wanghao
 */
public class ServerMessageHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(ServerMessageHandler.class);

    private Stream stream;

    @Override
    public void handleVideo(VideoMessage msg) {
        if (stream == null) {
            return;
        }
        stream.onRecvVideo(msg);
    }

    @Override
    public void handleAudio(AudioMessage msg) {
        if (stream == null) {
            return;
        }
        stream.onRecvAudio(msg);
    }

    @Override
    public void handleText(TextMessage msg) {
        if (stream == null) {
            return;
        }
        stream.onRecvText(msg);
    }

    @Override
    public void handleAMF0Data(ChannelHandlerContext ctx, AMF0DataMessage data) {
        if (data == null) {
            return;
        }
        List<Object> dataList = data.getDataList();
        if ("@setDataFrame".equals(dataList.get(0))) {
            Map<String, Object> metaData = (Map<String, Object>) dataList.get(2);
            if (stream != null) {
                stream.setMetaData(metaData);
            }
        }
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


    private void doHandleAMF0Command(ChannelHandlerContext ctx, final String commandName, final AMF0CommandMessage command) {
        logger.info("<-- recv command: " + commandName);
        switch (commandName) {
            case "connect":
                doHandleConnect(ctx, command);
                return;
            case "releaseStream":
                doHandleReleaseStream(ctx, command);
                return;
            case "FCPublish":
                doHandleFCPublish(ctx, command);
                return;
            case "createStream":
                doHandleCreateStream(ctx, command);
                return;
            case "publish":
                doHandlePublish(ctx, command);
                return;
            case "FCUnpublish":
                return;
            case "deleteStream":
                return;
            case "play":
                doHandlePlay(ctx, command);
                return;
            default:
                return;
        }
    }

    private void doHandlePlay(final ChannelHandlerContext ctx, final AMF0CommandMessage command) {
        String streamName = (String) command.getObjectList().get(3);
        if (streamName == null) {
            logger.error("invalid play message");
            return;
        }
        int idx = streamName.indexOf('?');
        if (idx != -1) {
            streamName = streamName.substring(0, idx);
        }
        StreamId streamId = new StreamId(APP_NAME, streamName);
        Stream stream = StreamManager.getStream(streamId);
        if (stream == null) {
            logger.error("stream=" + streamId + " not found");
            return;
        }

        // send streamBegin
        logger.info("--> send streamBegin");
        UserControlMessage streamBegin = new UserControlMessage((short) 0, 0);
        ctx.channel().writeAndFlush(streamBegin);

        // send onStatus('NetStream.Play.Start')
        logger.info("--> send onStatus('NetStream.Play.Start')");
        List<Object> onStatus = new ArrayList<>();
        onStatus.add("onStatus");
        onStatus.add(0);
        onStatus.add(null);
        AMF0Object amf0Object = new AMF0Object();
        amf0Object.put("level", "status");
        amf0Object.put("code", "NetStream.Play.Start");
        amf0Object.put("description", "Start live");
        onStatus.add(amf0Object);
        AMF0CommandMessage onStatusCommand = new AMF0CommandMessage(command.getCsid(), onStatus);
        ctx.channel().writeAndFlush(onStatusCommand);

        // send |RtmpSampleAccess()
        logger.info("--> send |RtmpSampleAccess()");
        List<Object> sampleAccess = new ArrayList<>();
        sampleAccess.add("|RtmpSampleAccess");
        sampleAccess.add(true);
        sampleAccess.add(true);
        AMF0DataMessage sampleAccessDataMessage = new AMF0DataMessage(command.getCsid(), sampleAccess);
        ctx.channel().writeAndFlush(sampleAccessDataMessage);

        // send onMetaData()
        logger.info("--> send onMetaData()");
        List<Object> onMetaData = new ArrayList<>();
        onMetaData.add("onMetaData");
        AMF0Object amf0Object1 = new AMF0Object();
        if (stream.getMetaData() != null) {
            amf0Object1.putAll(stream.getMetaData());
        }
        onMetaData.add(amf0Object1);
        AMF0DataMessage dataMessage = new AMF0DataMessage(command.getCsid(), onMetaData);
        ctx.channel().writeAndFlush(dataMessage);

        // start playing
        logger.info(streamId + " is playing");
        stream.addPlayer(ctx.channel());
    }

    private void doHandlePublish(final ChannelHandlerContext ctx, final AMF0CommandMessage command) {
        String streamName = (String) command.getObjectList().get(3);
        String appName = (String) command.getObjectList().get(4);
        if (appName == null || streamName == null) {
            logger.error("invalid publish message");
            return;
        }
        if (!APP_NAME.equals(appName)) {
            logger.error("app: " + appName + " not support");
            return;
        }
        int idx = streamName.indexOf('?');
        if (idx != -1) {
            streamName = streamName.substring(0, idx);
        }
        StreamId streamId = new StreamId(appName, streamName);
        Stream newStream = new Stream(streamId, ctx.channel());
        boolean createSuccess = StreamManager.createStream(streamId, newStream);
        if (!createSuccess) {
            logger.error("refuse duplicated stream=" + streamId);
            return;
        }
        stream = newStream;
        logger.info(streamId + " is publishing");

        // send onFCPublish()
        logger.info("--> send onFCPublish() for publish");
        List<Object> onFCPublish = new ArrayList<>();
        onFCPublish.add("onFCPublish");
        onFCPublish.add(0);
        onFCPublish.add(null);
        AMF0Object amf0Object = new AMF0Object();
        amf0Object.put("code", "NetStream.Publish.Start");
        amf0Object.put("description", "Started publishing stream.");
        onFCPublish.add(amf0Object);
        AMF0CommandMessage onFCPublishCommand = new AMF0CommandMessage(command.getCsid(), onFCPublish);
        ctx.channel().writeAndFlush(onFCPublishCommand);

        // send onStatus('NetStream.Publish.Start')
        logger.info("--> send onStatus('NetStream.Publish.Start')");
        List<Object> onStatus = new ArrayList<>();
        onStatus.add("onStatus");
        onStatus.add(0);
        onStatus.add(null);
        AMF0Object amf0Object1 = new AMF0Object();
        amf0Object1.put("level", "status");
        amf0Object1.put("code", "NetStream.Publish.Start");
        amf0Object1.put("description", "Started publishing stream.");
        amf0Object1.put("clientid", "ASAICiss");
        onStatus.add(amf0Object1);
        AMF0CommandMessage onStatusCommand = new AMF0CommandMessage(command.getCsid(), onStatus);
        ctx.channel().writeAndFlush(onStatusCommand);
    }

    private void doHandleCreateStream(final ChannelHandlerContext ctx, final AMF0CommandMessage command) {
        Object number = getNumber(command.getObjectList());
        // send _result
        logger.info("--> send _result for createStream");
        List<Object> _result = new ArrayList<>();
        _result.add("_result");
        _result.add(number);
        _result.add(null);
        AMF0CommandMessage _resultCommand = new AMF0CommandMessage(command.getCsid(), _result);
        ctx.channel().writeAndFlush(_resultCommand);
    }

    private void doHandleFCPublish(final ChannelHandlerContext ctx, final AMF0CommandMessage command) {
        Object number = getNumber(command.getObjectList());
        // send _result
        logger.info("--> send _result for FCPublish");
        List<Object> _result = new ArrayList<>();
        _result.add("_result");
        _result.add(number);
        _result.add(null);
        _result.add(null); //should be undefined
        AMF0CommandMessage _resultCommand = new AMF0CommandMessage(command.getCsid(), _result);
        ctx.channel().writeAndFlush(_resultCommand);
    }


    private void doHandleReleaseStream(final ChannelHandlerContext ctx, final AMF0CommandMessage command) {
        Object number = getNumber(command.getObjectList());
        // send _result
        logger.info("--> send _result for releaseStream");
        List<Object> _result = new ArrayList<>();
        _result.add("_result");
        _result.add(number);
        _result.add(null);
        _result.add(null); //should be undefined
        AMF0CommandMessage _resultCommand = new AMF0CommandMessage(command.getCsid(), _result);
        ctx.channel().writeAndFlush(_resultCommand);
    }

    private void doHandleConnect(final ChannelHandlerContext ctx, final AMF0CommandMessage command) {
        // send Window Acknowledgement Size
        logger.info("--> send Window Acknowledgement Size for connect");
        WindowAcknowledgementSize was = new WindowAcknowledgementSize(250000);
        ctx.channel().writeAndFlush(was);

        // send SetPeerBandwidth
        logger.info("--> send SetPeerBandwidth for connect");
        SetPeerBandwidth setPeerBandwidth = new SetPeerBandwidth(2500000, (byte) 2);
        ctx.channel().writeAndFlush(setPeerBandwidth);

        // send SetChunkSize
        logger.info("--> send SetChunkSize for connect");
        SetChunkSize setChunkSize = new SetChunkSize(4096);
        ctx.channel().writeAndFlush(setChunkSize);

        Object number = getNumber(command.getObjectList());
        // send _result('NetConnection.Connect.Success')
        logger.info("--> send _result('NetConnection.Connect.Success') for connect");
        List<Object> _result = new ArrayList<>();
        _result.add("_result");
        _result.add(number);
        AMF0Object amf0Object1 = new AMF0Object();
        amf0Object1.put("fmsVer", "FMS/3,5,3,888");
        amf0Object1.put("capabilities", 127);
        amf0Object1.put("mode", 1);
        AMF0Object amf0Object2 = new AMF0Object();
        amf0Object2.put("level", "status");
        amf0Object2.put("code", "NetConnection.Connect.Success");
        amf0Object2.put("description", "Connection succeeded");
        amf0Object2.put("objectEncoding", 0);
        _result.add(amf0Object1);
        _result.add(amf0Object2);
        AMF0CommandMessage _resultCommand = new AMF0CommandMessage(command.getCsid(), _result);
        ctx.channel().writeAndFlush(_resultCommand);

        // send onBWDone()
        logger.info("--> send onBWDone() for connect");
        List<Object> onBWDone = new ArrayList<>();
        onBWDone.add("onBWDone");
        onBWDone.add(number);
        onBWDone.add(null);
        AMF0CommandMessage onBWDoneCommand = new AMF0CommandMessage(command.getCsid(), onBWDone);
        ctx.channel().writeAndFlush(onBWDoneCommand);
    }



    private Object getNumber(List<Object> decodedObjectList) {
        Object number = 0;
        for (Object o : decodedObjectList) {
            int type = AMF0.getType(o);
            if (type == AMF0.Number) {
                number = o;
                break;
            }
        }
        return number;
    }
}
