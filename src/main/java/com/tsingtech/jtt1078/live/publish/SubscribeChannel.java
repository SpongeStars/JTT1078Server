package com.tsingtech.jtt1078.live.publish;

import com.tsingtech.jtt1078.live.subscriber.Subscriber;
import com.tsingtech.jtt1078.vo.DataPacket;
import com.tsingtech.jtt1078.vo.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:08
 * Mail: gwarmdll@gmail.com
 */
public class SubscribeChannel {

    private final String channel;

    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    private LocalDateTime time = LocalDateTime.now();

    public void setSequenceHeader(ByteBuf sequenceHeader) {
        this.sequenceHeader = sequenceHeader;
    }

    private ByteBuf sequenceHeader;

    private AtomicBoolean isDestory = new AtomicBoolean(false);

    public SubscribeChannel (String channel) {
        this.channel = channel;
    }

    public SubscribeChannel subscribe (Subscriber subscriber) {
        time = LocalDateTime.now();
        if (sequenceHeader != null) {
            subscriber.getChannel().writeAndFlush(sequenceHeader, subscriber.getChannel().voidPromise());
        }
        subscribers.add(subscriber);
        return this;
    }

    public boolean hasInitSequenceHeader () {
        return sequenceHeader == null;
    }

    public LocalDateTime getTime () {
        return time;
    }

    public boolean updateStatus (boolean expect, boolean update) {
        return isDestory.compareAndSet(expect, update);
    }

    public boolean getStatus () {
        return isDestory.get();
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
        subscribers.forEach(subscriber -> subscriber.getChannel().close(subscriber.getChannel().voidPromise()));
        ReferenceCountUtil.safeRelease(sequenceHeader);
    }
}
