package com.safety.platform.scheduler;

import com.safety.platform.service.StreamsSlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.streams.sla.enabled", havingValue = "true", matchIfMissing = true)
public class StreamsSlaScheduler {

    private final StreamsSlaService streamsSlaService;

    @Scheduled(fixedDelayString = "${app.streams.sla.monitor-fixed-delay-ms:10000}")
    public void captureSla() {
        streamsSlaService.captureLatestRecoveryIfNeeded();
    }
}

