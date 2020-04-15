package com.tsingtech.jtt1078.live.publish;

import com.tsingtech.jtt1078.live.subscriber.AudioSubscriber;
import com.tsingtech.jtt1078.live.subscriber.Subscriber;
import com.tsingtech.jtt1078.live.subscriber.VideoSubscriber;
import com.tsingtech.jtt1078.vo.AudioPacket;
import com.tsingtech.jtt1078.vo.VideoPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:08
 * Mail: gwarmdll@gmail.com
 */
public enum PublishManager {
    INSTANCE;

    private final static ConcurrentMap<String, SubscribeChannel> channels = new ConcurrentHashMap<>();

    public SubscribeChannel getSubscribeChannel (String streamId) {
        SubscribeChannel subscribeChannel = channels.get(streamId);
        if (subscribeChannel == null) {
            subscribeChannel = new SubscribeChannel(streamId);
            SubscribeChannel pre = channels.putIfAbsent(streamId, subscribeChannel);
            if (pre == null) {
                return subscribeChannel;
            } else {
                return channels.get(streamId);
            }
        }
        return subscribeChannel;
    }

    public void subscribe (Subscriber subscriber) {
        getSubscribeChannel(subscriber.getStreamId()).subscribe(subscriber);
    }

//    private void releaseSubscribeChannel(String streamId, SubscribeChannel subscribeChannel) {
//        if (channels.remove(streamId, subscribeChannel)) {
//            subscribeChannel.destroySubscribes();
//        }
//    }

    public void destroySingleSubscribeChannel(String streamId) {
        Optional.ofNullable(channels.remove(streamId)).ifPresent(SubscribeChannel::destroySubscribes);
    }

    public void unSubscribe (Subscriber subscriber) {
        SubscribeChannel subscribeChannel = channels.get(subscriber.getStreamId());
        if (subscribeChannel != null) {
            subscribeChannel.unSubscribe(subscriber);
        }
    }

    public void publish(String streamId, AudioPacket dataPacket) {
        try {
            channels.get(streamId).publishFrame(dataPacket);
        } finally {
            ReferenceCountUtil.safeRelease(dataPacket.getBody());
        }
    }

    public void publish(String streamId, VideoPacket dataPacket) {
        try {
            channels.get(streamId).publishFrame(dataPacket);
        } finally {
            ReferenceCountUtil.safeRelease(dataPacket.getBody());
        }
    }

    public void storeSequenceHeader(String streamId, ByteBuf sequenceHeader) {
        channels.get(streamId).setSequenceHeader(sequenceHeader);
    }

    public boolean hasInitSequenceHeader(String streamId) {
        SubscribeChannel subscribeChannel = channels.get(streamId);
        if (subscribeChannel != null) {
            return subscribeChannel.hasInitSequenceHeader();
        }
        return false;
    }

    public void initSubscribeChannel (String streamId, Channel channel) {
        SubscribeChannel subscribeChannel = getSubscribeChannel(streamId);
        subscribeChannel.setChannelType(AudioSubscriber.class);
        subscribeChannel.setDeviceChannel(channel);
    }

    public void initSubscribeChannel (String streamId, EventLoop eventLoop) {
        SubscribeChannel subscribeChannel = getSubscribeChannel(streamId);
        subscribeChannel.setChannelType(VideoSubscriber.class);
        subscribeChannel.setEventLoop(eventLoop);
    }

    public void publish2Device(String streamId, ByteBuf dataPacket) {
        channels.get(streamId).publish2Device(dataPacket);
    }

    public double getDuration(String streamId) {
       return channels.get(streamId).getDuration();
    }

}
