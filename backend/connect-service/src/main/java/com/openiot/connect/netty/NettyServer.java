package com.openiot.connect.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Netty TCP 服务器
 * 支持高并发设备连接
 */
@Slf4j
@Component
public class NettyServer {

    @Value("${netty.port:8888}")
    private int port;

    @Value("${netty.boss-threads:1}")
    private int bossThreads;

    @Value("${netty.worker-threads:0}")
    private int workerThreads;

    @Value("${netty.so-backlog:1024}")
    private int soBacklog;

    @Value("${netty.so-sndbuf:65535}")
    private int soSndbuf;

    @Value("${netty.so-rcvbuf:65535}")
    private int soRcvbuf;

    @Value("${netty.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${netty.max-frame-length:65535}")
    private int maxFrameLength;

    @Value("${netty.write-buffer-low-water-mark:32768}")
    private int writeBufferLowWaterMark;

    @Value("${netty.write-buffer-high-water-mark:65536}")
    private int writeBufferHighWaterMark;

    @Value("${netty.biz-threads:0}")
    private int bizThreads;

    @Value("${netty.reader-idle-time:120}")
    private int readerIdleTime;

    @Value("${netty.writer-idle-time:120}")
    private int writerIdleTime;

    @Value("${netty.all-idle-time:300}")
    private int allIdleTime;

    @Autowired
    private TcpMessageHandler messageHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventExecutorGroup bizGroup;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            // boss 线程组：处理连接请求
            bossGroup = new NioEventLoopGroup(bossThreads);

            // worker 线程组：处理 I/O 操作
            // 默认 0 表示使用 Netty 默认值 (CPU 核心数 * 2)
            workerGroup = new NioEventLoopGroup(workerThreads);
            // 业务线程池：隔离解析/鉴权/Kafka 发送，避免阻塞 I/O 线程
            bizGroup = new DefaultEventExecutorGroup(
                    bizThreads > 0 ? bizThreads : Math.max(4, Runtime.getRuntime().availableProcessors())
            );

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        // 服务端配置
                        .option(ChannelOption.SO_BACKLOG, soBacklog)
                        .option(ChannelOption.SO_REUSEADDR, true)
                        // 客户端连接配置
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.SO_SNDBUF, soSndbuf)
                        .childOption(ChannelOption.SO_RCVBUF, soRcvbuf)
                        .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                                new WriteBufferWaterMark(writeBufferLowWaterMark, writeBufferHighWaterMark))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        // 空闲检测（读空闲、写空闲、全空闲）
                                        .addLast("idle", new IdleStateHandler(
                                                readerIdleTime, writerIdleTime, allIdleTime))
                                        // 长度字段解码器（解决粘包/拆包问题）
                                        .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                                                maxFrameLength, 0, 4, 0, 4))
                                        // 长度字段编码器（出站响应与入站协议保持一致）
                                        .addLast("frameEncoder", new LengthFieldPrepender(4))
                                        // 字符串编解码器
                                        .addLast("decoder", new StringDecoder(CharsetUtil.UTF_8))
                                        .addLast("encoder", new StringEncoder(CharsetUtil.UTF_8))
                                        // 业务处理器
                                        .addLast(bizGroup, "handler", messageHandler);
                            }
                        });

                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("Netty TCP Server 启动成功 - 端口: {}, Boss线程: {}, Worker线程: {}",
                        port, bossThreads, workerThreads == 0 ? "自动" : workerThreads);
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty Server 启动失败", e);
            } finally {
                shutdown();
            }
        }, "netty-server").start();
    }

    @PreDestroy
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bizGroup != null) {
            bizGroup.shutdownGracefully();
        }
        log.info("Netty Server 已关闭");
    }
}
