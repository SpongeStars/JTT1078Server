package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.DataPacket;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

import java.time.LocalDateTime;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/7 10:10
 */
public abstract class AbstractMediaMessageHandler<T extends DataPacket> extends SimpleChannelInboundHandler<T> {
    protected String streamId;
    private T dataPacket;
    private CompositeByteBuf compositeByteBuf;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T subPacket) throws Exception {
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
                publish(dataPacket);
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
                dataPacket.setBody(compositeByteBuf
                        .addComponent(true, subPacket.getBody()));
                publish(dataPacket);
                dataPacket = null;
                compositeByteBuf = null;
                break;
            case 3:
                compositeByteBuf.addComponent(true, subPacket.getBody());
                break;
            default:
                ReferenceCountUtil.safeRelease(subPacket.getBody());
                System.out.println("unknow packet type");
        }
    }

    public void close() {
        PublishManager.INSTANCE.releaseSingleChannel(streamId);
        if (compositeByteBuf != null) {
            ReferenceCountUtil.safeRelease(compositeByteBuf);
        }
    }

    protected abstract void publish(T dataPacket);
}
