package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.VideoPacket;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/1 13:29
 */
@Slf4j
public class VideoMessageHandler extends AbstractMediaMessageHandler<VideoPacket> {
    @Override
    protected void init(ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("A new device channel init and start to publish video, streamId = {}.", streamId);
        }
        PublishManager.INSTANCE.initSubscribeChannel(streamId, ctx.channel().eventLoop());
    }

    @Override
    protected void publish(VideoPacket dataPacket) {
        PublishManager.INSTANCE.publish(streamId, dataPacket);
    }
}
