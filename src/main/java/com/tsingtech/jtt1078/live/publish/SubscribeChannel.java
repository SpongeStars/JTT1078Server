package com.tsingtech.jtt1078.live.publish;

import com.tsingtech.jtt1078.live.subscriber.Subscriber;
import com.tsingtech.jtt1078.vo.DataPacket;
import com.tsingtech.jtt1078.vo.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:08
 * Mail: gwarmdll@gmail.com
 */
public class SubscribeChannel {

    private final String channel;

    // Signature(3 Byte)+Version(1 Byte)+Flags(1 Bypte)+DataOffset(4 Byte)
    private static final ByteBuf flvHeader = Unpooled.directBuffer(9)
            .writeBytes(new byte[]{ 0x46, 0x4c, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09 });

    private Channel producer;

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

    public SubscribeChannel subscribe (Subscriber subscriber) {
        subscriber.getChannel().writeAndFlush(flvHeader.retainedDuplicate());
        if (sequenceHeader != null) {
            subscriber.getChannel().writeAndFlush(sequenceHeader.retainedDuplicate(), subscriber.getChannel().voidPromise());
        }
        subscribers.add(subscriber);
        return this;
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

    public void registerProducer(Channel channel) {
        this.producer = channel;
        this.eventLoop = channel.eventLoop();
    }

    public void p(ByteBuf byteBuf) {
        if (this.producer != null) {
            this.producer.writeAndFlush(byteBuf);
        } else {
            ReferenceCountUtil.safeRelease(byteBuf);
        }
    }
}
