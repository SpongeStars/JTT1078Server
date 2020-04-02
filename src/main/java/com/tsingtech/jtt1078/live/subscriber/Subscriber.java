package com.tsingtech.jtt1078.live.subscriber;

import io.netty.channel.Channel;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:11
 * Mail: gwarmdll@gmail.com
 */
public interface Subscriber {

    String getStreamId ();
    Channel getChannel ();
}
