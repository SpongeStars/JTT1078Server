package com.tsingtech.jtt1078.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Map;

import static org.springframework.cloud.commons.util.IdUtils.getDefaultInstanceId;

/**
 * @author chrisliu
 * @mail chrisliu.top@gmail.com
 * @since 2020/4/10 11:17
 */
@Configuration
public class EurekaConnfigure {
    @Value("${tsing-jtt1078.server.livePort}")
    private Integer port;

    private ConfigurableEnvironment env;

    public EurekaConnfigure(ConfigurableEnvironment env) {
        this.env = env;
    }

    @Bean
    public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils,
                                                             ManagementMetadataProvider managementMetadataProvider) {
        String hostname = getProperty("eureka.instance.hostname");
        boolean preferIpAddress = Boolean
                .parseBoolean(getProperty("eureka.instance.prefer-ip-address"));
        String ipAddress = getProperty("eureka.instance.ip-address");
        boolean isSecurePortEnabled = Boolean
                .parseBoolean(getProperty("eureka.instance.secure-port-enabled"));

        String serverContextPath = env.getProperty("server.servlet.context-path", "/");

        Integer managementPort = env.getProperty("management.server.port", Integer.class); // nullable.
        // should
        // be
        // wrapped
        // into
        // optional
        String managementContextPath = env
                .getProperty("management.server.servlet.context-path"); // nullable.
        // should
        // be wrapped into
        // optional
        Integer jmxPort = env.getProperty("com.sun.management.jmxremote.port",
                Integer.class); // nullable
        EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean(inetUtils);

        instance.setNonSecurePort(port);
        instance.setInstanceId(getDefaultInstanceId(env));
        instance.setPreferIpAddress(preferIpAddress);
        instance.setSecurePortEnabled(isSecurePortEnabled);
        if (StringUtils.hasText(ipAddress)) {
            instance.setIpAddress(ipAddress);
        }

        if (isSecurePortEnabled) {
            instance.setSecurePort(port);
        }

        if (StringUtils.hasText(hostname)) {
            instance.setHostname(hostname);
        }
        String statusPageUrlPath = getProperty("eureka.instance.status-page-url-path");
        String healthCheckUrlPath = getProperty("eureka.instance.health-check-url-path");

        if (StringUtils.hasText(statusPageUrlPath)) {
            instance.setStatusPageUrlPath(statusPageUrlPath);
        }
        if (StringUtils.hasText(healthCheckUrlPath)) {
            instance.setHealthCheckUrlPath(healthCheckUrlPath);
        }

        ManagementMetadata metadata = managementMetadataProvider.get(instance, port,
                serverContextPath, managementContextPath, managementPort);

        if (metadata != null) {
            instance.setStatusPageUrl(metadata.getStatusPageUrl());
            instance.setHealthCheckUrl(metadata.getHealthCheckUrl());
            if (instance.isSecurePortEnabled()) {
                instance.setSecureHealthCheckUrl(metadata.getSecureHealthCheckUrl());
            }
            Map<String, String> metadataMap = instance.getMetadataMap();
            metadataMap.computeIfAbsent("management.port",
                    k -> String.valueOf(metadata.getManagementPort()));
        }
        else {
            // without the metadata the status and health check URLs will not be set
            // and the status page and health check url paths will not include the
            // context path so set them here
            if (StringUtils.hasText(managementContextPath)) {
                instance.setHealthCheckUrlPath(
                        managementContextPath + instance.getHealthCheckUrlPath());
                instance.setStatusPageUrlPath(
                        managementContextPath + instance.getStatusPageUrlPath());
            }
        }

        setupJmxPort(instance, jmxPort);
        return instance;
    }

    private void setupJmxPort(EurekaInstanceConfigBean instance, Integer jmxPort) {
        Map<String, String> metadataMap = instance.getMetadataMap();
        if (metadataMap.get("jmx.port") == null && jmxPort != null) {
            metadataMap.put("jmx.port", String.valueOf(jmxPort));
        }
    }

    private String getProperty(String property) {
        return this.env.containsProperty(property) ? this.env.getProperty(property) : "";
    }
}
