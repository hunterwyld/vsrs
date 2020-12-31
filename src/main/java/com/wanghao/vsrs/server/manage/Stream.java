package com.wanghao.vsrs.server.manage;

import com.wanghao.vsrs.common.rtmp.message.AudioMessage;
import com.wanghao.vsrs.common.rtmp.message.RtmpMessage;
import com.wanghao.vsrs.common.rtmp.message.TextMessage;
import com.wanghao.vsrs.common.rtmp.message.VideoMessage;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Map;

/**
 * @author wanghao
 */
public class Stream {
    private static final Logger logger = Logger.getLogger(Stream.class);

    private StreamId streamId;

    private Channel publisher;
    private LinkedList<Channel> players;

    private LinkedList<RtmpMessage> gopCache;

    private Map<String, Object> metaData;

    // AVCDecoderConfigurationRecord defined in ISO-14496-15 AVC file format
    private VideoMessage avcSequenceHeader;
    // AudioSpecificConfig defined in ISO-14496-3 Audio
    private AudioMessage aacSequenceHeader;


    public Stream(StreamId streamId, Channel publisher) {
        this.streamId = streamId;
        this.publisher = publisher;
        this.players = new LinkedList<>();
        this.gopCache = new LinkedList<>();
    }

    public synchronized void addPlayer(Channel player) {
        if (player != null && player.isActive()) {
            players.add(player);

            // write AVC/AAC Sequence Header first
            if (avcSequenceHeader != null) {
                player.writeAndFlush(avcSequenceHeader);
            }
            if (aacSequenceHeader != null) {
                player.writeAndFlush(aacSequenceHeader);
            }
            // write gop cache then
            for (RtmpMessage msg : gopCache) {
                player.writeAndFlush(msg);
            }
        }
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public synchronized void onRecvVideo(VideoMessage msg) {
        if (msg.isAVCSequenceHeader()) {
            logger.info("<-- recv AVC Sequence Header, stream=" + streamId);
            avcSequenceHeader = msg;
        }
        if (msg.isH264KeyFrame()) {
            logger.info("<-- recv key frame, stream=" + streamId);
            gopCache.clear();
        }

        gopCache.add(msg);
        broadcastToPlayers(msg);
    }

    public synchronized void onRecvAudio(AudioMessage msg) {
        if (msg.isAACSequenceHeader()) {
            logger.info("<-- recv AAC Sequence Header, stream=" + streamId);
            aacSequenceHeader = msg;
        }

        gopCache.add(msg);
        broadcastToPlayers(msg);
    }

    public void onRecvText(TextMessage msg) {
        broadcastToPlayers(msg);
    }

    private void broadcastToPlayers(RtmpMessage msg) {
        for (Channel player : players) {
            if (player.isActive()) {
                player.writeAndFlush(msg);
            }
        }
    }

    public Channel getPublisher() {
        return publisher;
    }
}
