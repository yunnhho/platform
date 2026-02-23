package com.safety.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage-tier")
@Getter
@Setter
public class StorageTierProperties {

    private boolean enabled = true;
    private int hotHours = 24;
    private int warmDays = 7;
    private int batchSize = 500;
    private boolean archiveEnabled = true;
    private String archiveCron = "0 */10 * * * *";
}

