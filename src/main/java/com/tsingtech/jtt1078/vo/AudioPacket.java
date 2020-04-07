package com.tsingtech.jtt1078.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Author: chrisliu
 * Date: 2019/6/26 14:46
 * Mail: gwarmdll@gmail.com
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class AudioPacket extends DataPacket {
    private byte _audioFlag;

    @Override
    public DataPacket newInstance() {
        return new AudioPacket();
    }
}
