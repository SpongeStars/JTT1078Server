package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.AudioPacket;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/7 10:19
 */
public class AudioMessageHandler extends AbstractMediaMessageHandler<AudioPacket> {
    @Override
    protected void publish(AudioPacket dataPacket) {
        PublishManager.INSTANCE.publish(streamId, dataPacket);
    }

    @Override
    protected void init(ChannelHandlerContext ctx) {
        PublishManager.INSTANCE.registerProducer(streamId, ctx.channel());
    }
}
