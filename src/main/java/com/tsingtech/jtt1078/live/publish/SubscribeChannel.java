package com.tsingtech.jtt1078.live.publish;

import com.tsingtech.jtt1078.live.subscriber.AudioSubscriber;
import com.tsingtech.jtt1078.live.subscriber.Subscriber;
import com.tsingtech.jtt1078.live.subscriber.VideoSubscriber;
import com.tsingtech.jtt1078.vo.DataPacket;
import com.tsingtech.jtt1078.vo.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:08
 * Mail: gwarmdll@gmail.com
 */
@Slf4j
public class SubscribeChannel {

    private final String channel;

    private double duration;

    public void setChannelType(Class<? extends Subscriber> channelType) {
        this.channelType = channelType;
    }

    private Class<? extends Subscriber> channelType;

    // Signature(3 Byte)+Version(1 Byte)+Flags(1 Bypte)+DataOffset(4 Byte)
    private static final ByteBuf flvHeader = Unpooled.directBuffer(9)
            .writeBytes(new byte[]{ 0x46, 0x4c, 0x56, 0x01, 0x01, 0x00, 0x00,
                    0x00, 0x09, 0x00, 0x00, 0x00, 0x00 });

    private Channel deviceChannel;

    public EventLoop getEventLoop() {
        return eventLoop;
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    private EventLoop eventLoop;

    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    public void setSequenceHeader(ByteBuf sequenceHeader) {
        if (sequenceHeaderStatus.compareAndSet(0, 1)) {
            this.sequenceHeader = sequenceHeader;
        } else {
            ReferenceCountUtil.safeRelease(sequenceHeader);
        }
    }

    private ByteBuf sequenceHeader = null;

    AtomicInteger sequenceHeaderStatus = new AtomicInteger(0);

    public SubscribeChannel (String channel) {
        this.channel = channel;
    }

    public void subscribe (Subscriber subscriber) {
        if (channelType != null) {
            if (!channelType.isInstance(subscriber)) {
                log.warn("SubscribeChannel received not match subscriber, type = {}, streamId = {}, now close the subscriber.",
                        channelType.getName(), subscriber.getStreamId());
                subscriber.getChannel().close();
                return ;
            }
        }


        if (subscriber instanceof VideoSubscriber) {
            subscriber.getChannel().writeAndFlush(new BinaryWebSocketFrame(flvHeader.retainedDuplicate()));
            if (sequenceHeader != null) {
                subscriber.getChannel().writeAndFlush(new BinaryWebSocketFrame(sequenceHeader.retainedDuplicate()), subscriber.getChannel().voidPromise());
            }
        } else {
            duration = ((AudioSubscriber)subscriber).getDuration();
        }
        if (log.isDebugEnabled()) {
            log.debug("SubscribeChannel receive subscriber, type = {}, streamId = {}.",
                    channelType != null ? channelType.getSimpleName() : subscriber.getClass().getSimpleName(),
                    subscriber.getStreamId());
        }
        subscribers.add(subscriber);
    }

    public boolean hasInitSequenceHeader () {
        return sequenceHeader != null;
    }

    public void unSubscribe (Subscriber subscriber) {
        subscribers.remove(subscriber);
    }

    public void publishFrame (DataPacket dataPacket) {
        subscribers.forEach(subscriber -> {
            if (subscriber.getChannel().isWritable()) {
                subscriber.getChannel().writeAndFlush(new PacketWrapper(dataPacket,
                                dataPacket.getBody().slice().retain()),
                        subscriber.getChannel().voidPromise());
            }
        });
    }

    public void destroySubscribes () {
        subscribers.forEach(subscriber -> {
            if (subscriber.getChannel().isOpen()) {
                subscriber.getChannel().close();
            }
        });
        subscribers.clear();
        Optional.ofNullable(sequenceHeader).ifPresent(ReferenceCountUtil::safeRelease);
    }

    public void setDeviceChannel (Channel channel) {
        this.deviceChannel = channel;
        this.eventLoop = channel.eventLoop();
    }

    public void publish2Device(ByteBuf byteBuf) {
        if (this.deviceChannel != null) {
            this.deviceChannel.writeAndFlush(byteBuf);
        } else {
            ReferenceCountUtil.safeRelease(byteBuf);
        }
    }

    public double getDuration () {
        return duration;
    }
}
