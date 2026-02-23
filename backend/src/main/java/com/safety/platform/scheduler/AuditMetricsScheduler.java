package com.safety.platform.scheduler;

import com.safety.platform.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditMetricsScheduler {

    private final AuditService auditService;

    @Scheduled(fixedDelay = 30000)
    public void syncAuditMetric() {
        auditService.syncImmutabilityMetric();
    }
}
