package com.tsingtech.jtt1078.live.publish;

import com.tsingtech.jtt1078.live.subscriber.Subscriber;
import com.tsingtech.jtt1078.vo.DataPacket;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

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

    public void subscribe (Subscriber subscriber) {
        SubscribeChannel subscribeChannel = channels.get(subscriber.getStreamId());
        if (subscribeChannel == null) {
            subscribeChannel = new SubscribeChannel(subscriber.getStreamId()).subscribe(subscriber);
            if ((subscribeChannel = channels.putIfAbsent(subscriber.getStreamId(), subscribeChannel)) != null) {
                subscribeChannel.subscribe(subscriber);
            }
        } else {
            if (!subscribeChannel.getStatus()) {
                subscribeChannel.subscribe(subscriber);
            } else {
                releaseSubscribeChannel(subscriber.getStreamId(), subscribeChannel);
                subscribeChannel = new SubscribeChannel(subscriber.getStreamId()).subscribe(subscriber);
                if ((subscribeChannel = channels.putIfAbsent(subscriber.getStreamId(), subscribeChannel)) != null) {
                    subscribeChannel.subscribe(subscriber);
                }
            }
        }
    }

    private void releaseSubscribeChannel(String streamId, SubscribeChannel subscribeChannel) {
        if (channels.remove(streamId, subscribeChannel)) {
            subscribeChannel.destorySubscribes();
        }
    }

    public void releaseSingleChannel(String streamId) {
        SubscribeChannel subscribeChannel = channels.remove(streamId);
        if (subscribeChannel != null) {
            subscribeChannel.destorySubscribes();
        }
    }

    public void unSubscribe (Subscriber subscriber) {
        SubscribeChannel subscribeChannel = channels.get(subscriber.getStreamId());
        if (subscribeChannel != null) {
            subscribeChannel.unSubscribe(subscriber);
        }
    }

    public void publish(String streamId, DataPacket dataPacket) {
        SubscribeChannel subscribeChannel = channels.get(streamId);
        try {
            if (subscribeChannel != null) {
                subscribeChannel.publishFrame(dataPacket);
            }
        } finally {
            ReferenceCountUtil.safeRelease(dataPacket.getBody());
        }
    }

    public void storeSequenceHeader(String streamId, ByteBuf sequenceHeader) {
        SubscribeChannel subscribeChannel = channels.get(streamId);
        if (subscribeChannel != null) {
            subscribeChannel.setSequenceHeader(sequenceHeader);
        }
    }

    public boolean hasInitSequenceHeader(String streamId) {
        SubscribeChannel subscribeChannel = channels.get(streamId);
        if (subscribeChannel != null) {
            return subscribeChannel.hasInitSequenceHeader();
        }
        return false;
    }

}
