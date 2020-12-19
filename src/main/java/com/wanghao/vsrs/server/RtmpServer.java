package com.wanghao.vsrs.server;

import com.wanghao.vsrs.server.handler.ChunkDecoder;
import com.wanghao.vsrs.server.handler.ChunkEncoder;
import com.wanghao.vsrs.server.handler.HandshakeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * @author wanghao
 */
public class RtmpServer {
    private static final Logger logger = Logger.getLogger(RtmpServer.class);

    private static int port = 1935;

    private static final boolean epollAvailable = Epoll.isAvailable();
    private static final boolean kqueueAvailable = KQueue.isAvailable();

    public static void main(String[] args) throws InterruptedException {
        if(args != null && args.length > 1) {
            if(args[0].equalsIgnoreCase("-p") || args[0].equalsIgnoreCase("-port")) {
                port = Integer.parseInt(args[1]);
            }
        }

        // 创建mainReactor线程池
        EventLoopGroup bossGroup = selectGroup();
        // 创建工作线程池
        EventLoopGroup workerGroup = selectGroup();
        try {
            final ServerBootstrap bootstrap = new ServerBootstrap()
                    // 组装EventLoopGroup
                    .group(bossGroup, workerGroup)
                    // 设置channel类型
                    .channel(selectChannel())
                    // 设置端口
                    .localAddress(new InetSocketAddress(port))
                    // 开启TCP no delay，允许较小数据包的发送
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 配置入站、出站事件handler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            sc.pipeline()
                            .addLast(new HandshakeHandler())
                            .addLast(new ChunkDecoder())
                            .addLast(new ChunkEncoder())
                            ;
                        }
                    });

            // 异步的绑定服务器，调用sync()方法阻塞等待直到绑定完成
            ChannelFuture f = bootstrap.bind().sync();

            logger.info("Rtmp server started on port " + port);

            // 一直阻塞，除非遇到InterruptedException
            f.channel().closeFuture().sync();
        } finally {
            // 关闭EventLoopGroup，释放所有的资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static EventLoopGroup selectGroup() {
        if (epollAvailable) {
            return new EpollEventLoopGroup();
        }
        if (kqueueAvailable) {
            return new KQueueEventLoopGroup();
        }
        return new NioEventLoopGroup();
    }

    private static Class<? extends ServerChannel> selectChannel() {
        if (epollAvailable) {
            return EpollServerSocketChannel.class;
        }
        if (kqueueAvailable) {
            return KQueueServerSocketChannel.class;
        }
        return NioServerSocketChannel.class;
    }
}
