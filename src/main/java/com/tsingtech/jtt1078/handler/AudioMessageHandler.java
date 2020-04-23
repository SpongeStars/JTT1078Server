package com.tsingtech.jtt1078.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.vo.AudioPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/7 10:19
 */
@Slf4j
public class AudioMessageHandler extends AbstractMediaMessageHandler<AudioPacket> {

    private byte[] simRaw;
    private byte PT;
    private byte logicChannel;
    private byte typeFlag;
    private int sequenceNum = 0;

    private double duration = 0;

    private static final byte[] separators = new byte[]{0x30, 0x31, 0x63, 0x64, (byte) 0x81};

    private boolean isHaisi;

    @Override
    protected void publish(AudioPacket dataPacket) {
        if (isHaisi) {
            dataPacket.setBody(dataPacket.getBody().slice(4, dataPacket.getBody().readableBytes() - 4));
//            dataPacket.getBody().skipBytes(4);
//            System.out.println(dataPacket.getBody().readableBytes());
//            buffer.put(dataPacket.getBody().nioBuffer());
//            buffer.force();
        }
        PublishManager.INSTANCE.publish(streamId, dataPacket);
    }
//    MappedByteBuffer buffer;
    @Override
    protected void init(ChannelHandlerContext ctx) {
//        RandomAccessFile raf = null;
//        try {
//            raf = new RandomAccessFile("D:/015211539978_1_g726", "rw");
//            FileChannel channel = raf.getChannel();
//            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 70 * 1024);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        simRaw = dataPacket.getSimRaw();
        PT = dataPacket.getPTRaw();
        logicChannel= dataPacket.getLogicChannel();
        typeFlag = dataPacket.getTypeFlag();
        if (log.isDebugEnabled()) {
            log.debug("A new device channel init and start to publish audio, streamId = {}, PT = {}",
                    streamId, PT & 0x7f);
        }
        byte[] haisiHeader = new byte[4];
        dataPacket.getBody().getBytes(0, haisiHeader);
        if (haisiHeader[0] == 0x00 && haisiHeader[1] == 0x01 && haisiHeader[3] == 0x00
                && haisiHeader[2] * 2 == dataPacket.getBody().readableBytes() - 4) {
            this.isHaisi = true;
        }

        PublishManager.INSTANCE.initSubscribeChannel(streamId, ctx.channel());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (match) {
            if (duration <= 0) {
                duration = PublishManager.INSTANCE.getDuration(streamId);
            }
            ByteBuf data = (ByteBuf) msg;
            ctx.write(ctx.alloc().directBuffer(26).writeBytes(separators).writeByte(PT).writeShort(sequenceNum)
            .writeBytes(simRaw).writeByte(logicChannel).writeByte(typeFlag).writeLong((long) (sequenceNum * duration))
            .writeShort(data.readableBytes()));
            ctx.writeAndFlush(data);
            sequenceNum++;
        } else {
            ctx.write(msg, promise);
        }
    }
}
