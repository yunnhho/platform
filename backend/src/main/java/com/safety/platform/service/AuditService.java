package com.safety.platform.service;

import com.safety.platform.domain.EventAuditLog;
import com.safety.platform.dto.AuditStatsResponse;
import com.safety.platform.repository.EventAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final EventAuditLogRepository auditLogRepository;
    private final MetricsService metricsService;
    private final AtomicLong lastCount = new AtomicLong(0);

    public Page<EventAuditLog> getModificationAttempts(Pageable pageable) {
        return auditLogRepository.findAllByOrderByAttemptedAtDesc(pageable);
    }

    public AuditStatsResponse getStatistics() {
        long updateAttempts = auditLogRepository.countByOperation("UPDATE");
        long deleteAttempts = auditLogRepository.countByOperation("DELETE");

        return AuditStatsResponse.builder()
            .totalAttempts(updateAttempts + deleteAttempts)
            .updateAttempts(updateAttempts)
            .deleteAttempts(deleteAttempts)
            .lastAttemptedAt(
                auditLogRepository.findTopByOrderByAttemptedAtDesc()
                    .map(EventAuditLog::getAttemptedAt)
                    .orElse(null)
            )
            .build();
    }

    @Transactional
    public void syncImmutabilityMetric() {
        long current = auditLogRepository.count();
        long previous = lastCount.getAndSet(current);
        long delta = Math.max(0, current - previous);
        for (long i = 0; i < delta; i++) {
            metricsService.recordImmutabilityViolation();
        }
    }
}
