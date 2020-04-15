package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.live.publish.SubscribeChannel;
import com.tsingtech.jtt1078.vo.DataPacket;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/7 10:10
 */
@Slf4j
public abstract class AbstractMediaMessageHandler<I extends DataPacket> extends ChannelDuplexHandler {
    protected String streamId;
    protected I dataPacket;
    private CompositeByteBuf compositeByteBuf;
    protected boolean match = false;

    private final TypeParameterMatcher matcher;

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     */
    protected AbstractMediaMessageHandler() {
        matcher = TypeParameterMatcher.find(this, AbstractMediaMessageHandler.class, "I");
    }

    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (streamId == null) {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                I imsg = (I) msg;
                dataPacket = imsg;
                match= true;
                streamId = String.join("/", imsg.getSim(), String.valueOf(imsg.getLogicChannel()));
                SubscribeChannel subscribeChannel = PublishManager.INSTANCE.getSubscribeChannel(streamId);
                subscribeChannel.setEventLoop(ctx.channel().eventLoop());
                init(ctx);
                channelRead0(ctx, imsg);
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            @SuppressWarnings("unchecked")
            I imsg = (I) msg;
            channelRead0(ctx, imsg);
        }

    }

    protected abstract void init(ChannelHandlerContext ctx);

    protected void channelRead0(ChannelHandlerContext ctx, I subPacket) throws Exception {
        switch(subPacket.getPacketPlace()) {
            case 0:
                if (compositeByteBuf != null) {
                    ReferenceCountUtil.safeRelease(compositeByteBuf);
                }
                subPacket.setStreamId(streamId);
                publish(subPacket);
                break;
            case 1:
                dataPacket = subPacket;
                if (compositeByteBuf != null) {
                    ReferenceCountUtil.safeRelease(compositeByteBuf);
                }
                compositeByteBuf = ctx.channel().alloc().compositeDirectBuffer(130)
                        .addComponent(true, subPacket.getBody());
                break;
            case 2:
                dataPacket.setStreamId(streamId);
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
                log.warn("unknow packet type, type = {}", subPacket.getPacketPlace());
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (match) {
            log.info("Remote device client closed, streamId = {}", streamId);
            release();
        }
        ctx.close(promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (match) {
            log.info("Remote device client came to inactive, streamId = {}", streamId);
            release();
        }

        ctx.fireChannelInactive();
    }

    private void release() {
        PublishManager.INSTANCE.destroySingleSubscribeChannel(streamId);
        if (compositeByteBuf != null) {
            ReferenceCountUtil.safeRelease(compositeByteBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
        if (match) {
            log.warn("catch exception, streamId = {}", streamId);
            release();
        }
    }

    protected abstract void publish(I dataPacket);
}
