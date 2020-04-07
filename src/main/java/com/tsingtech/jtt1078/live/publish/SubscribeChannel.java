package com.tsingtech.jtt1078.live.publish;

import com.tsingtech.jtt1078.live.subscriber.Subscriber;
import com.tsingtech.jtt1078.vo.DataPacket;
import com.tsingtech.jtt1078.vo.PacketWrapper;
import io.netty.buffer.ByteBuf;
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
        if (sequenceHeader != null) {
            sequenceHeader.retain();
            subscriber.getChannel().writeAndFlush(sequenceHeader.slice(), subscriber.getChannel().voidPromise());
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

    public void destorySubscribes () {
        subscribers.forEach(subscriber -> subscriber.getChannel().close());
        Optional.ofNullable(sequenceHeader).ifPresent(ReferenceCountUtil::safeRelease);
    }
}
