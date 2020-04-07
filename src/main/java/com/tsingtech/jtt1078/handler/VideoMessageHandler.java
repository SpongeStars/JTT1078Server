package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.VideoPacket;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/1 13:29
 */
public class VideoMessageHandler extends AbstractMediaMessageHandler<VideoPacket> {
    @Override
    protected void publish(VideoPacket dataPacket) {
        PublishManager.INSTANCE.publish(streamId, dataPacket);
    }
}
