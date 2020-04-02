package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.AudioPacket;
import com.tsingtech.jtt1078.vo.DataPacket;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

import java.time.LocalDateTime;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/1 13:29
 */
public class MediaMessageHandler extends ChannelDuplexHandler {

    private String streamId;
    private DataPacket dataPacket;
    private CompositeByteBuf compositeByteBuf;

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
        PublishManager.INSTANCE.releaseSingleChannel(streamId);
        ReferenceCountUtil.safeRelease(compositeByteBuf);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AudioPacket) {
            ctx.fireChannelRead(msg);
            return;
        }
        DataPacket subPacket = (DataPacket) msg;
        if (streamId == null) {
            streamId = String.join("/", subPacket.getSim(), String.valueOf(subPacket.getLogicChannel()));
            System.out.println(streamId);
        }
        switch(subPacket.getPacketPlace()) {
            case 0:
                if (compositeByteBuf != null) {
                    ReferenceCountUtil.safeRelease(compositeByteBuf);
                }
                subPacket.setStreamId(streamId);
                if (subPacket.getBody().getByte(0) != 0) {
                    System.out.println("====");
                }
                PublishManager.INSTANCE.publish(streamId, subPacket);
                break;
            case 1:
                dataPacket = subPacket;
                if (compositeByteBuf != null) {
                    ReferenceCountUtil.safeRelease(compositeByteBuf);
                }
                compositeByteBuf = ctx.channel().alloc().compositeDirectBuffer(130)
                        .addComponent(true, subPacket.getBody());
                if (compositeByteBuf.getByte(0) != 0) {
                    System.out.println("====");
                }
                break;
            case 2:
                dataPacket.setStreamId(streamId);
                if (compositeByteBuf.getByte(compositeByteBuf.readerIndex()) != 0) {
                    System.out.println("====");
                }
                System.out.println(LocalDateTime.now());
                PublishManager.INSTANCE.publish(streamId, dataPacket.setBody(compositeByteBuf
                        .addComponent(true, subPacket.getBody())));
                dataPacket = null;
                compositeByteBuf = null;
                break;
            case 3:
                compositeByteBuf.addComponent(true, subPacket.getBody());
                break;
            default:
                ReferenceCountUtil.safeRelease(subPacket.getBody());
                throw new RuntimeException("unknow packet type");
        }
    }
}
