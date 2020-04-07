package com.tsingtech.jtt1078.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Author: chrisliu
 * Date: 2019/8/24 17:13
 * Mail: gwarmdll@gmail.com
 */
@Component
@Data
@ConfigurationProperties(JTT1078ServerProperties.PREFIX)
public class JTT1078ServerProperties {

    public static final String PREFIX = "tsing-jtt1078.server";

    String host;
    Integer port;
    String app;
    Integer livePort;
}
