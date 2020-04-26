package com.tsingtech.jtt1078.live.handler;

import com.tsingtech.jtt1078.live.publish.PublishManager;
import com.tsingtech.jtt1078.live.subscriber.AbstractSubscriber;
import com.tsingtech.jtt1078.vo.AudioPacket;
import com.tsingtech.jtt1078.vo.PacketWrapper;
import com.tsingtech.jtt1078.vo.VideoPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * Author: chrisliu
 * Date: 2020-03-02 09:19
 * Mail: gwarmdll@gmail.com
 */
public class SrsFlvMuxerHandler extends ChannelOutboundHandlerAdapter {
    private int naluSeparatorSize;
    private String streamId;

    public SrsFlvMuxerHandler (String streamId) {
        this.streamId = streamId;
    }

    // E.4.3.1 VIDEODATA
    // Frame Type UB [4]
    // Type of video frame. The following values are defined:
    //     1 = key frame (for AVC, a seekable frame)
    //     2 = inter frame (for AVC, a non-seekable frame)
    //     3 = disposable inter frame (H.263 only)
    //     4 = generated key frame (reserved for server use only)
    //     5 = video info/command frame
    public static class SrsCodecVideoAVCFrame
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;
        public final static int Reserved1 = 6;

        public final static int KeyFrame                     = 1;
        public final static int InterFrame                 = 2;
        public final static int DisposableInterFrame         = 3;
        public final static int GeneratedKeyFrame            = 4;
        public final static int VideoInfoFrame                = 5;
    }

    // AVCPacketType IF CodecID == 7 UI8
    // The following values are defined:
    //     0 = AVC sequence header
    //     1 = AVC NALU
    //     2 = AVC end of sequence (lower level NALU sequence ender is
    //         not required or supported)
    public static class SrsCodecVideoAVCType
    {
        // set to the max value to reserved, for array map.
        public final static int Reserved                    = 3;

        public final static int SequenceHeader                 = 0;
        public final static int NALU                         = 1;
        public final static int SequenceHeaderEOF             = 2;
    }

    /**
     * E.4.1 FLV Tag, page 75
     */
    public static class SrsCodecFlvTag
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved = 0;

        // 8 = audio
        public final static int Audio = 8;
        // 9 = video
        public final static int Video = 9;
        // 18 = script data
        public final static int Script = 18;
    };

    // E.4.3.1 VIDEODATA
    // CodecID UB [4]
    // Codec Identifier. The following values are defined:
    //     2 = Sorenson H.263
    //     3 = Screen video
    //     4 = On2 VP6
    //     5 = On2 VP6 with alpha channel
    //     6 = Screen video version 2
    //     7 = AVC
    public static class SrsCodecVideo
    {
        // set to the zero to reserved, for array map.
        public final static int Reserved                = 0;
        public final static int Reserved1                = 1;
        public final static int Reserved2                = 9;

        // for user to disable video, for example, use pure audio hls.
        public final static int Disabled                = 8;

        public final static int SorensonH263             = 2;
        public final static int ScreenVideo             = 3;
        public final static int On2VP6                 = 4;
        public final static int On2VP6WithAlphaChannel = 5;
        public final static int ScreenVideoVersion2     = 6;
        public final static int AVC                     = 7;
    }

    /**
     * Table 7-1 – NAL unit type codes, syntax element categories, and NAL unit type classes
     * H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 83.
     */
    public static class SrsAvcNaluType
    {
        // Unspecified
        public final static int Reserved = 0;

        // Coded slice of a non-IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int NonIDR = 1;
        // Coded slice data partition A slice_data_partition_a_layer_rbsp( )
        public final static int DataPartitionA = 2;
        // Coded slice data partition B slice_data_partition_b_layer_rbsp( )
        public final static int DataPartitionB = 3;
        // Coded slice data partition C slice_data_partition_c_layer_rbsp( )
        public final static int DataPartitionC = 4;
        // Coded slice of an IDR picture slice_layer_without_partitioning_rbsp( )
        public final static int IDR = 5;
        // Supplemental enhancement information (SEI) sei_rbsp( )
        public final static int SEI = 6;
        // Sequence parameter set seq_parameter_set_rbsp( )
        public final static int SPS = 7;
        // Picture parameter set pic_parameter_set_rbsp( )
        public final static int PPS = 8;
        // Access unit delimiter access_unit_delimiter_rbsp( )
        public final static int AccessUnitDelimiter = 9;
        // End of sequence end_of_seq_rbsp( )
        public final static int EOSequence = 10;
        // End of stream end_of_stream_rbsp( )
        public final static int EOStream = 11;
        // Filler data filler_data_rbsp( )
        public final static int FilterData = 12;
        // Sequence parameter set extension seq_parameter_set_extension_rbsp( )
        public final static int SPSExt = 13;
        // Prefix NAL unit prefix_nal_unit_rbsp( )
        public final static int PrefixNALU = 14;
        // Subset sequence parameter set subset_seq_parameter_set_rbsp( )
        public final static int SubsetSPS = 15;
        // Coded slice of an auxiliary coded picture without partitioning slice_layer_without_partitioning_rbsp( )
        public final static int LayerWithoutPartition = 19;
        // Coded slice extension slice_layer_extension_rbsp( )
        public final static int CodedSliceExt = 20;
    }

    private static final long maxOffsetTimestamp = Integer.MAX_VALUE;
    private static final byte zeroByte = (byte)0x00;

    private int searchAnnexbFrame (ByteBuf data, int src) {
        int index = src;
        for (;;) {
            if (index - src > 20) {
                return 0;
            }
            if (data.getByte(index) == zeroByte && data.getByte(index + 1) == zeroByte) {
                // match N[00] 00 00 01, where N>=0
                if (data.getByte(index + 2) == (byte)0x01) {
                    naluSeparatorSize = 3;
                    return index + 3;
                }
                if (data.getByte(index + 2) == zeroByte && data.getByte(index + 3) == (byte)0x01) {
                    naluSeparatorSize = 4;
                    return index + 4;
                }

            }

            index++;
        }
    }

    private static void muxVideoDataHeader(ByteBuf fvlTag, int frame_type,
                                           int avc_packet_type, int cts) {
        // for h264 in RTMP video payload, there is 5bytes header:
        //      1bytes, FrameType | CodecID
        //      1bytes, AVCPacketType
        //      3bytes, CompositionTime, the cts.

        // @see: E.4.3 Video Tags, video_file_format_spec_v10_1.pdf, page 78
        // Frame Type, Type of video frame.
        // CodecID, Codec Identifier.
        // set the rtmp header
        fvlTag.writeByte(((frame_type << 4) | SrsCodecVideo.AVC));

        // AVCPacketType
        fvlTag.writeByte(avc_packet_type);

        // CompositionTime
        // pts = dts + cts, or
        // cts = pts - dts.
        // where cts is the header in rtmp video packet payload header.
//        int cts = pts - dts;
        fvlTag.writeByte(cts >> 16);
        fvlTag.writeByte(cts >> 8);
        fvlTag.writeByte(cts);
    }

    private static ByteBuf muxNalusFlvHeader(ByteBufAllocator allocator, int frameType, int dts, int datasize, int cts) {
        ByteBuf flvTag = allocator.directBuffer(20);
        muxVideoTagHeader(flvTag, datasize + 9, dts);
        muxVideoDataHeader(flvTag, frameType, SrsCodecVideoAVCType.NALU, cts);
        flvTag.writeInt(datasize);
        return flvTag;
    }

    private static ByteBuf muxSequenceHeaderFlvTag(ByteBufAllocator allocator, ByteBuf sps, ByteBuf pps) {
        int dataSize = 5 + sps.readableBytes() + pps.readableBytes() + 11;
        ByteBuf flvTag = allocator.directBuffer(11 + dataSize);

        muxVideoTagHeader(flvTag, dataSize, 0);

        muxVideoDataHeader(flvTag, SrsCodecVideoAVCFrame.KeyFrame, SrsCodecVideoAVCType.SequenceHeader, 0);

        // h.264 raw data.
        muxAVCDecorderConfigurationRecord(flvTag, sps, pps);
//        System.out.println(String.format("flv: h264 sps/pps sent, sps=%dB, pps=%dB",
//                sps.capacity(), pps.capacity()));
        flvTag.writeInt(11 + dataSize);
        return flvTag;
    }

    private static void muxVideoTagHeader(ByteBuf flvTag, int dataSize, int dts) {
        flvTag.writeByte(0x09);
        flvTag.writeMedium(dataSize);
        flvTag.writeMedium(dts);
        flvTag.writeByte(dts >> 24);
        flvTag.writeMedium(0);
    }

    private static void muxAVCDecorderConfigurationRecord(ByteBuf sequenceHeader, ByteBuf sps, ByteBuf pps) {
        // 5bytes sps/pps header:
        //      configurationVersion, AVCProfileIndication, profile_compatibility,
        //      AVCLevelIndication, lengthSizeMinusOne
        // 3bytes size of sps:
        //      numOfSequenceParameterSets, sequenceParameterSetLength(2B)
        // Nbytes of sps.
        //      sequenceParameterSetNALUnit
        // 3bytes size of pps:
        //      numOfPictureParameterSets, pictureParameterSetLength
        // Nbytes of pps:
        //      pictureParameterSetNALUnit

        // decode the SPS:
        // @see: 7.3.2.1.1, H.264-AVC-ISO_IEC_14496-10-2012.pdf, page 62


        // @see: Annex A Profiles and levels, H.264-AVC-ISO_IEC_14496-10.pdf, page 205
        //      Baseline profile profile_idc is 66(0x42).
        //      Main profile profile_idc is 77(0x4d).
        //      Extended profile profile_idc is 88(0x58).
        byte profile_idc = sps.getByte(1);
        //u_int8_t constraint_set = frame[2];
        byte level_idc = sps.getByte(3);

        // generate the sps/pps header
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // configurationVersion
        sequenceHeader.writeByte(0x01);
        // AVCProfileIndication
        sequenceHeader.writeByte(profile_idc);
        // profile_compatibility
        sequenceHeader.writeByte(0x00);
        // AVCLevelIndication
        sequenceHeader.writeByte(level_idc);
        // lengthSizeMinusOne, or NAL_unit_length, always use 4bytes size,
        // so we always set it to 0x03.
        sequenceHeader.writeByte(0x03);

        // sps
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // numOfSequenceParameterSets, always 1
        sequenceHeader.writeByte(0x01);
        // sequenceParameterSetLength
        sequenceHeader.writeShort(sps.readableBytes());
        // sequenceParameterSetNALUnit
        sequenceHeader.writeBytes(sps);


        // pps
        // 5.3.4.2.1 Syntax, H.264-AVC-ISO_IEC_14496-15.pdf, page 16
        // numOfPictureParameterSets, always 1
        sequenceHeader.writeByte(0x01);
        // pictureParameterSetLength
        sequenceHeader.writeShort(pps.readableBytes());

        // pictureParameterSetNALUnit
        sequenceHeader.writeBytes(pps);
    }

    private BinaryWebSocketFrame buildTagFrame(ByteBufAllocator allocator, int codecVideoAVCFrame, int dts, ByteBuf data, int cts) {
        return new BinaryWebSocketFrame(allocator.compositeBuffer(3)
                .addComponent(true, muxNalusFlvHeader(allocator, codecVideoAVCFrame, dts, data.readableBytes(), cts))
                .addComponent(true, data)
                .addComponent(true, allocator.directBuffer(4).writeInt(20 + data.readableBytes()))
        );
    }

    private final static TypeParameterMatcher matcher = TypeParameterMatcher.get(PacketWrapper.class);

    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!acceptInboundMessage(msg)) {
            ctx.write(msg);
            return;
        }


        PacketWrapper packetWrapper = (PacketWrapper) msg;
        if (packetWrapper.getPacket() instanceof AudioPacket) {
            ctx.write(new BinaryWebSocketFrame(packetWrapper.getBody()), ctx.voidPromise());
            return;
        }
        VideoPacket videoPacket = (VideoPacket) packetWrapper.getPacket();
        ByteBuf data = packetWrapper.getBody();
        int index = data.readerIndex();
        index = searchAnnexbFrame(data, index);
        if (index != 0) {
            int offsetTimestamp;
            if (videoPacket.getTimestamp() > maxOffsetTimestamp) {
                offsetTimestamp = (int) (maxOffsetTimestamp & videoPacket.getTimestamp());
            } else {
                offsetTimestamp = (int) videoPacket.getTimestamp();
            }
            int type = data.getByte(index) & 0x1f;
            if (type == SrsAvcNaluType.SPS) {
                int spsIndex = index;
                index = searchAnnexbFrame(data, index);
                int spsLength = index - spsIndex - naluSeparatorSize;
                int ppsIndex = index;
                index = searchAnnexbFrame(data, index);
                int ppsLength = index - ppsIndex - naluSeparatorSize;
                if (!PublishManager.INSTANCE.hasInitSequenceHeader(videoPacket.getStreamId())) {
                    ByteBuf sequenceHeader = muxSequenceHeaderFlvTag(ctx.alloc(), data.slice(spsIndex, spsLength), data.slice(ppsIndex, ppsLength));
                    sequenceHeader.retain();
                    ctx.write(new BinaryWebSocketFrame(sequenceHeader)).addListener(f -> {
                        if (f.isSuccess()) {
                            PublishManager.INSTANCE.storeSequenceHeader(videoPacket.getStreamId(),sequenceHeader);
                        } else {
                            ReferenceCountUtil.safeRelease(sequenceHeader);
                        }
                    });
                }
                if ((data.getByte(index) & 0x1f) == SrsAvcNaluType.SEI) {
                    index = searchAnnexbFrame(data, index);
                }
                ByteBuf frame = data.slice(index, data.readableBytes() - index);
                if (frame.readableBytes() > 113000) {
                    System.out.println("exceed max frame?");
                }
                ctx.writeAndFlush(buildTagFrame(ctx.alloc(), SrsCodecVideoAVCFrame.KeyFrame, offsetTimestamp, frame, videoPacket.getLFI()), ctx.voidPromise());
            } else if (type == SrsAvcNaluType.NonIDR) {
                ByteBuf frame = data.slice(index, data.readableBytes() - index);
                ctx.writeAndFlush(buildTagFrame(ctx.alloc(), SrsCodecVideoAVCFrame.InterFrame, offsetTimestamp, frame, videoPacket.getLFI()), ctx.voidPromise());
            } else {
                System.out.println(ByteBufUtil.hexDump(data, 0, data.readableBytes()));

                ReferenceCountUtil.safeRelease(data);
            }
        } else {
            System.out.println("no annexb frame");
            ReferenceCountUtil.safeRelease(data);
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
        AttributeKey<AbstractSubscriber> subscriberKey = AttributeKey.valueOf("subscriber");
        PublishManager.INSTANCE.unSubscribe(ctx.channel().attr(subscriberKey).get());
    }
}
