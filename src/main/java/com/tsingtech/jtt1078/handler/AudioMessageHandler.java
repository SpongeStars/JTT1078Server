package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.AudioPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/7 10:19
 */
public class AudioMessageHandler extends AbstractMediaMessageHandler<AudioPacket> {

    private byte[] simRaw;
    private byte PT;
    private byte logicChannel;
    private byte typeFlag;
    private int sequenceNum = 0;

    private static byte[] separators = new byte[]{0x30, 0x31, 0x63, 0x64, (byte) 0x81};

    @Override
    protected void publish(AudioPacket dataPacket) {
        PublishManager.INSTANCE.publish(streamId, dataPacket);
    }

    @Override
    protected void init(ChannelHandlerContext ctx) {
        simRaw = dataPacket.getSimRaw();
        PT = dataPacket.getPT();
        logicChannel= dataPacket.getLogicChannel();
        typeFlag = dataPacket.getTypeFlag();
        PublishManager.INSTANCE.registerProducer(streamId, ctx.channel());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (match) {
            ByteBuf data = (ByteBuf) msg;
            ctx.write(ctx.alloc().directBuffer(26).writeBytes(separators).writeByte(PT).writeShort(sequenceNum)
            .writeBytes(simRaw).writeByte(logicChannel).writeByte(typeFlag).writeLong((long) (sequenceNum * 0.064))
            .writeShort(data.readableBytes()));
            ctx.writeAndFlush(data);
            sequenceNum++;
        } else {
            ctx.write(msg, promise);
        }
    }

}
