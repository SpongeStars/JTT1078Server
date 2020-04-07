package com.tsingtech.jtt1078.standard;

import com.tsingtech.jtt1078.config.JTT1078ServerProperties;
import com.tsingtech.jtt1078.handler.Jtt1078ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Author: chrisliu
 * Date: 2019/8/24 11:37
 * Mail: gwarmdll@gmail.com
 */
@Component
public class JTT1078Server implements SmartInitializingSingleton {

    @Autowired
    JTT1078ServerProperties JTT1078ServerProperties;

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    public void init() {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(1024, 1024 * 7, 1024 * 12))
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 1024 * 1024 * 2))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new Jtt1078ServerChannelInitializer());

        ChannelFuture channelFuture;
        channelFuture = bootstrap.bind(JTT1078ServerProperties.getPort());
        bootstrap.bind(1079);

        channelFuture.addListener(future -> {
            if (!future.isSuccess()){
                future.cause().printStackTrace();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }
}
