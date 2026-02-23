package com.safety.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.streams.sla")
@Getter
@Setter
public class StreamsSlaProperties {

    private boolean enabled = true;
    private long thresholdMs = 30000L;
    private long monitorFixedDelayMs = 10000L;
}

