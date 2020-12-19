package com.wanghao.vsrs.server.manage;

import com.wanghao.vsrs.common.rtmp.message.AudioMessage;
import com.wanghao.vsrs.common.rtmp.message.RtmpMessage;
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

    private LinkedList<RtmpMessage> mediaData;

    private Map<String, Object> metaData;

    // FLV 文件中第一个 VIDEOTAG 的 VIDEODATA 的 AVCVIDEOPACKET 的 Data
    // 总是 AVCDecoderConfigurationRecord（在 ISO/IEC 14496-15 中定义），
    // 解码的时候注意跳过这个 VIDOETAG。
    private VideoMessage avcDecoderConfigurationRecord;


    public Stream(StreamId streamId, Channel publisher) {
        this.streamId = streamId;
        this.publisher = publisher;
        this.players = new LinkedList<>();
        this.mediaData = new LinkedList<>();
    }

    public synchronized void addPlayer(Channel player) {
        if (player != null && player.isActive()) {
            players.add(player);

            // write avcDecoderConfigurationRecord first
            if (avcDecoderConfigurationRecord != null) {
                player.writeAndFlush(avcDecoderConfigurationRecord);
            }
            for (RtmpMessage msg : mediaData) {
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
        if (msg.isAVCDecoderConfigurationRecord()) {
            logger.info("<-- recv avcDecoderConfigurationRecord, stream=" + streamId);
            avcDecoderConfigurationRecord = msg;
        }
        if (msg.isH264KeyFrame()) {
            logger.info("<-- recv key frame, stream=" + streamId);
            mediaData.clear();
        }

        mediaData.add(msg);
        broadcastToPlayers(msg);
    }

    public synchronized void onRecvAudio(AudioMessage msg) {
        mediaData.add(msg);
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
