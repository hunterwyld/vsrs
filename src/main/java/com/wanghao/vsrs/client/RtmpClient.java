package com.wanghao.vsrs.client;

import com.wanghao.vsrs.client.handler.netty.HandshakeHandler;
import com.wanghao.vsrs.common.handler.netty.ChunkDecoder;
import com.wanghao.vsrs.common.handler.netty.ChunkEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wanghao.vsrs.client.conf.Properties.*;
import static com.wanghao.vsrs.common.util.Constant.APP_NAME;

/**
 * @author wanghao
 * @description
 */
public class RtmpClient {
    private static final Logger logger = Logger.getLogger(RtmpClient.class);

    // 客户端最大重连次数
    private static final int MAX_RECONNECT = 3;
    // 客户端已尝试重连次数
    private static int RECONNECT = 0;

    private static EventLoopGroup group;
    private static Bootstrap bootstrap;

    public static void main(String[] args) throws Exception {
        if (args.length != 2 && args.length != 3) {
            printUsage(new Exception("invalid number of args"));
        }
        String url = null;
        if ("-play".equals(args[0])) {
            isPlayer = true;
            publishFilePath = null;
            url = args[1];
        } else if ("-publish".equals(args[0])) {
            isPlayer = false;
            publishFilePath = args[1];
            url = args[2];
        } else {
            printUsage(new Exception("invalid args[0]"));
        }

        boolean success = parseUrl(url);
        if (!success) {
            throw new Exception("invalid rtmp url: " + url);
        }

        // 创建工作线程池
        group = new NioEventLoopGroup();
        // 创建mainReactor线程池
        bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(serverHost, serverPort)
                // 开启TCP no delay，允许较小数据包的发送
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new HandshakeHandler())
                                .addLast(new ChunkDecoder(false))
                                .addLast(new ChunkEncoder())
                                ;
                    }
                });

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

    private static boolean parseUrl(String url) {
        if (url == null) {
            return false;
        }
        final String regex = "^rtmp://(.+)/live/(.+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String ipPort = matcher.group(1);
            String[] ipPortSplit = ipPort.split(":");
            if (ipPortSplit.length == 1) {
                serverHost = ipPortSplit[0];
                serverPort = 1935;
            } else if (ipPortSplit.length == 2) {
                serverHost = ipPortSplit[0];
                serverPort = Integer.parseInt(ipPortSplit[1]);
            } else {
                return false;
            }
            appName = APP_NAME;
            streamName = matcher.group(2);
            return true;
        }
        return false;
    }

    private static void printUsage(Exception e) throws Exception {
        System.out.println("--------------- USAGE ---------------");
        System.out.println("Publish: java -jar rtmpclient.jar -publish text.json rtmp://127.0.0.1/live/test");
        System.out.println("Play: java -jar rtmpclient.jar -play rtmp://127.0.0.1/live/test");
        System.out.println("-------------------------------------");
        throw e;
    }
}
