package com.tsingtech.jtt1078.live.subscriber;

import io.netty.channel.Channel;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:02
 * Mail: gwarmdll@gmail.com
 */
public abstract class AbstractSubscriber implements Subscriber {

    protected Channel channel;
    protected String streamId;

    public AbstractSubscriber (Channel channel, String streamId) {
        this.channel = channel;
        this.streamId = streamId;
    }

    @Override
    public String getStreamId() {
        return this.streamId;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }
}
