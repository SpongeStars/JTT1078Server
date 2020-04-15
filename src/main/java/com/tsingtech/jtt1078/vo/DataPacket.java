package com.tsingtech.jtt1078.vo;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Author: chrisliu
 * Date: 2019/6/26 14:38
 * Mail: gwarmdll@gmail.com
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public abstract class DataPacket {

    protected byte[] simRaw;

    protected String streamId;

    protected double frameRate;

    public byte getPT() {
        return (byte) (PT & 0x7f);
    }

    protected byte PT;

    protected byte logicChannel;

    protected byte typeFlag;

    protected ByteBuf body;

    protected long timestamp;

    public int getPacketPlace () {
        return typeFlag & 0x0f;
    }

    public String getSim () {
        StringBuilder stringBuilder = new StringBuilder(12);
        for (byte single : simRaw) {
            stringBuilder.append((single >> 4) & 0x0f).append(single & 0x0f);
        }
        return stringBuilder.toString();
    }

    public abstract DataPacket newInstance ();
}
