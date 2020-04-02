package com.tsingtech.jtt1078.live.subscriber;

import io.netty.channel.Channel;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:02
 * Mail: gwarmdll@gmail.com
 */
public class VideoSubscriber extends AbstractSubscriber {

    public VideoSubscriber(Channel channel, String streamId) {
        super(channel, streamId);
    }
}
