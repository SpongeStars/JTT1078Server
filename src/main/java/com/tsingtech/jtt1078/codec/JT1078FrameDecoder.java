package com.tsingtech.jtt1078.codec;

import com.tsingtech.jtt1078.vo.AudioPacket;
import com.tsingtech.jtt1078.vo.DataPacket;
import com.tsingtech.jtt1078.vo.VideoPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Author: chrisliu
 * Date: 2019/8/25 11:03
 * Mail: gwarmdll@gmail.com
 * 纵有疾风起，人生不言弃
 */
public class JT1078FrameDecoder extends ReplayingDecoder<JT1078FrameDecoder.DecoderState> {

    public static final int PacketType_IFrame = 0x00;
    public static final int PacketType_PFrame = 0x01;
    public static final int PacketType_BFrame = 0x02;
    public static final int PacketType_AudioFrame = 0x03;
    public static final int PacketType_Transparent = 0x04;

    private static final int SimFieldOffset = 8;
    private static final int ChannelFieldOffset = 14;
    private static final int TimestampFieldOffset = 16;
    private static final int LFIFieldOffset = 26;

    private static final int PacketType_LengthFieldOffset = 15;
    private static final int Transparent_LengthFieldEndOffset = 18;
    private static final int AudioFrame_LengthFieldEndOffset = 24;
    private static final int VideoFrame_LengthFieldEndOffset = 30;

    private int frameLength;

    private boolean hasInit = false;
    private int offset;
    private int type = -1;


    MappedByteBuffer buffer;

    private DataPacket dataPacket;

    public JT1078FrameDecoder () {
        super(DecoderState.Decode_DeviceInfo);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("D:/611912120046_2.txt", "rw");
            FileChannel channel = raf.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 30240000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
        DecoderState state = super.state();
        switch(state) {
            case Decode_DeviceInfo:
                byte packetTypeField = in.getByte(in.readerIndex() + PacketType_LengthFieldOffset);
                type = (packetTypeField >> 4) & 0x0f;
                offset = getLengthFieldEndOffset(type);
                dataPacket.setTypeFlag(packetTypeField);
                if (!hasInit && dataPacket != null) {
                    byte[] simRaw = new byte[6];
                    in.getBytes(in.readerIndex() + SimFieldOffset, simRaw);
                    in.getByte(in.readerIndex() + ChannelFieldOffset);
                    dataPacket.setSimRaw(simRaw);
                    dataPacket.setLogicChannel(in.getByte(in.readerIndex() + ChannelFieldOffset));
                    hasInit = true;
//                    AttributeKey<String> sessionIdKey = AttributeKey.valueOf("streamId");
//                    channelHandlerContext.channel().attr(sessionIdKey).set(String.join("_", dataPacket.getSim(), String.valueOf(dataPacket.getLogicChannel())));
                }
                checkpoint(DecoderState.Decode_FrameInfo);
            case Decode_FrameInfo:
                frameLength = in.getUnsignedShort(in.readerIndex() + offset - 2);
                boolean isFirstbpackage = dataPacket.getPacketPlace() == 0 || dataPacket.getPacketPlace() == 1;
                if (type != PacketType_Transparent && isFirstbpackage) {
                    dataPacket.setTimestamp(in.getLong(in.readerIndex() + TimestampFieldOffset));
                }
                if (type < PacketType_AudioFrame && isFirstbpackage) {
                    ((VideoPacket)dataPacket).setLFI(in.getUnsignedShort(in.readerIndex() + LFIFieldOffset));
                }
                checkpoint(DecoderState.Decode_Payload);
            case Decode_Payload:
                if (dataPacket != null) {
                    list.add(dataPacket.setBody(in.slice(in.readerIndex() + offset, frameLength).retain()));
                }
//                buffer.put(dataPacket.getBody().nioBuffer());
////                buffer.force();
                in.skipBytes(frameLength + offset);
                dataPacket = null;
                checkpoint(DecoderState.Decode_DeviceInfo);
                break;
            default:
                throw new RuntimeException("unexpected decoder state: " + state);
        }
    }

    private int getLengthFieldEndOffset (int type) {
        if (type < PacketType_AudioFrame) {
            dataPacket = new VideoPacket();
            return VideoFrame_LengthFieldEndOffset;
        } else if (type == PacketType_AudioFrame) {
            dataPacket = new AudioPacket();
            return AudioFrame_LengthFieldEndOffset;
        } else if (type == PacketType_Transparent) {
            return Transparent_LengthFieldEndOffset;
        } else {
            throw new RuntimeException();
        }
    }

    public enum DecoderState {
        Decode_DeviceInfo,
        Decode_FrameInfo,
        Decode_Payload
    }
}
