package com.tsingtech.jtt1078.live.subscriber;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Author: chrisliu
 * Date: 2020-02-28 10:02
 * Mail: gwarmdll@gmail.com
 */
@Getter
@Setter
@Accessors(chain = true)
public class AudioSubscriber extends AbstractSubscriber {

    private double duration;

    public AudioSubscriber(Channel channel, String streamId, double duration) {
        super(channel, streamId);
        this.duration = duration;
    }
}
