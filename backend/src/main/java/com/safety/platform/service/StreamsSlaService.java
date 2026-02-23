package com.safety.platform.service;

import com.safety.platform.config.StreamsSlaProperties;
import com.safety.platform.domain.KafkaRecoverySlaLog;
import com.safety.platform.dto.StreamsSlaCheckItemResponse;
import com.safety.platform.dto.StreamsSlaStatusResponse;
import com.safety.platform.repository.KafkaRecoverySlaLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamsSlaService {

    private final MetricsService metricsService;
    private final KafkaRecoverySlaLogRepository slaLogRepository;
    private final StreamsSlaProperties streamsSlaProperties;

    private final AtomicLong lastObservedRecoveryDurationMs = new AtomicLong(-1L);

    @Transactional
    public void captureLatestRecoveryIfNeeded() {
        if (!streamsSlaProperties.isEnabled()) {
            return;
        }
        long recoveryDuration = metricsService.getLastRecoveryDurationMs();
        if (recoveryDuration <= 0) {
            return;
        }

        long previous = lastObservedRecoveryDurationMs.get();
        if (previous == recoveryDuration) {
            return;
        }
        if (!lastObservedRecoveryDurationMs.compareAndSet(previous, recoveryDuration)) {
            return;
        }

        long threshold = streamsSlaProperties.getThresholdMs();
        KafkaRecoverySlaLog logEntity = new KafkaRecoverySlaLog();
        logEntity.setCheckedAt(LocalDateTime.now());
        logEntity.setRecoveryDurationMs(recoveryDuration);
        logEntity.setThresholdMs(threshold);
        logEntity.setCompliant(recoveryDuration <= threshold);
        slaLogRepository.save(logEntity);
    }

    @Transactional(readOnly = true)
    public StreamsSlaStatusResponse getStatus() {
        long totalChecks = slaLogRepository.count();
        long passedChecks = slaLogRepository.countByCompliant(true);
        long failedChecks = Math.max(0L, totalChecks - passedChecks);
        var latest = slaLogRepository.findTopByOrderByCheckedAtDesc();
        List<StreamsSlaCheckItemResponse> recentChecks = slaLogRepository.findTop20ByOrderByCheckedAtDesc()
            .stream()
            .map(logEntity -> StreamsSlaCheckItemResponse.builder()
                .checkedAt(logEntity.getCheckedAt())
                .recoveryDurationMs(logEntity.getRecoveryDurationMs())
                .thresholdMs(logEntity.getThresholdMs())
                .compliant(logEntity.isCompliant())
                .build())
            .toList();

        return StreamsSlaStatusResponse.builder()
            .thresholdMs(streamsSlaProperties.getThresholdMs())
            .totalChecks(totalChecks)
            .passedChecks(passedChecks)
            .failedChecks(failedChecks)
            .latestRecoveryDurationMs(latest.map(KafkaRecoverySlaLog::getRecoveryDurationMs).orElse(null))
            .latestCompliant(latest.map(KafkaRecoverySlaLog::isCompliant).orElse(null))
            .latestCheckedAt(latest.map(KafkaRecoverySlaLog::getCheckedAt).orElse(null))
            .recentChecks(recentChecks)
            .build();
    }
}

