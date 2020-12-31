package com.wanghao.vsrs.common.util;

/**
 * @author wanghao
 */
public class Constant {
    // fmt is 0 and timestamp is 0xffffff, means the presence of Extended Timestamp
    // fmt is 1, 2 and timestamp delta is 0xffffff, means the presence of Extended Timestamp
    // fmt is 3 and ???
    public static final int RTMP_EXTENDED_TIMESTAMP = 0xffffff;

    // fmt in Chunk Basic Header
    public static final byte FMT_0 = 0x00;
    public static final byte FMT_1 = 0x01;
    public static final byte FMT_2 = 0x02;
    public static final byte FMT_3 = 0x03;

    /* --------------- RTMP Messages Begin --------------- */
    // Protocol Control Message
    public static final byte RTMP_MSG_SetChunkSize                  = 0x01;
    public static final byte RTMP_MSG_AbortMessage                  = 0x02;
    public static final byte RTMP_MSG_Acknowledgement               = 0x03;
    public static final byte RTMP_MSG_WindowAcknowledgementSize     = 0x05;
    public static final byte RTMP_MSG_SetPeerBandwidth              = 0x06;

    // User Control Message
    public static final byte RTMP_MSG_UserControlMessage            = 0x04;

    // Audio Message
    public static final byte RTMP_MSG_AudioMessage                  = 0x08;
    // Video Message
    public static final byte RTMP_MSG_VideoMessage                  = 0x09;
    // Text Message(self defined, not rtmp standard)
    public static final byte SELFDEFINED_MSG_TextMessage             = 0x0A;

    // Command Message
    public static final byte RTMP_MSG_AMF3CommandMessage            = 0x11;//17
    public static final byte RTMP_MSG_AMF0CommandMessage            = 0x14;//20

    // Data Message
    public static final byte RTMP_MSG_AMF3DataMessage               = 0x0F;//15
    public static final byte RTMP_MSG_AMF0DataMessage               = 0x12;//18

    // SharedObjectMessage
    public static final byte RTMP_MSG_AMF3SharedObjectMessage       = 0x10;//16
    public static final byte RTMP_MSG_AMF0SharedObjectMessage       = 0x13;//19

    // Aggregate Message
    public static final byte RTMP_MSG_AggregateMessage              = 0x16;//22
    /* --------------- RTMP Messages End --------------- */


    // Chunk Size
    public static final int RTMP_MIN_CHUNK_SIZE = 128;
    public static final int RTMP_MAX_CHUNK_SIZE = 65536;


    public static final int AUDIO_CONTROL = 175;//0xaf
    public static final int VIDEO_CONTROL_KEYFRAME = 23;//0x17
    public static final int VIDEO_CONTROL_INTERFRAME = 39;//0x27

    public static final int DEFAULT_VIDEO_OUT_CSID = 12;
    public static final int DEFAULT_AUDIO_OUT_CSID = 10;
    public static final int DEFAULT_TEXT_OUT_CSID = 14;

    public static final String APP_NAME = "live";

}
