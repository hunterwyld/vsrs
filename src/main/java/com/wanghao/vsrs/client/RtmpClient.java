package com.wanghao.vsrs.client;

import com.wanghao.vsrs.client.handler.HandshakeHandler;
import com.wanghao.vsrs.common.util.Utils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;

/**
 * @author wanghao
 * @description
 */
public class RtmpClient {
    private static final Logger logger = Logger.getLogger(RtmpClient.class);

    private static final int port = Utils.getRandomPortWithin(50000, 50100);

    // 客户端最大重连次数
    private static final int MAX_RECONNECT = 3;
    // 客户端已尝试重连次数
    private static int RECONNECT = 0;

    private static String serverHost;
    private static int serverPort;

    private static EventLoopGroup group;
    private static Bootstrap bootstrap;

    public static void main(String[] args) {

        String serverUrl = "rtmp://127.0.0.1/live/test";
        serverHost = "127.0.0.1";
        serverPort = 1935;

        // 创建工作线程池
        group = new NioEventLoopGroup();
        // 创建mainReactor线程池
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(serverHost, serverPort)
                // 开启TCP no delay，允许较小数据包的发送
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new HandshakeHandler());

        connect();
    }


    private static void connect() {
        bootstrap.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // 连接失败，尝试重连
                if (!future.isSuccess()) {
                    logger.info("Connect to " + serverHost + ":" + serverPort + " failed");
                    Thread.sleep(3000);
                    reconnect();
                    return;
                }

                logger.info("Connect to " + serverHost + ":" + serverPort + " success");
                // 连接成功，重试次数清零
                RECONNECT = 0;
            }
        });
    }

    private static void reconnect() {
        if (RECONNECT >= MAX_RECONNECT) {
            logger.info("reconnect times exhausted, give up...");
            group.shutdownGracefully();
            return;
        }

        RECONNECT++;
        connect();
    }
}
