package com.tsingtech.jtt1078.live.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Author: chrisliu
 * Date: 2020-03-02 09:19
 * Mail: gwarmdll@gmail.com
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    MappedByteBuffer buffer;
    private final String streamId;

    public WebSocketServerHandler(String streamId) {
        this.streamId = streamId;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("D:/611912120046_origin_2_audio", "rw");
            FileChannel channel = raf.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 10240000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        handleWebSocketFrame(ctx, msg);
    }

//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//
//    }
//
//    @Override
//    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//
//    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            ctx.writeAndFlush(frame.retainedDuplicate()).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            buffer.put(frame.content().nioBuffer());
            buffer.force();
            PublishManager.INSTANCE.publish2Device(streamId, frame.content().retain());
        }
    }

}
