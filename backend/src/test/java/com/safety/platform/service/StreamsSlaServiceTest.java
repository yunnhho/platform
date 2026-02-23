package com.safety.platform.service;

import com.safety.platform.config.StreamsSlaProperties;
import com.safety.platform.domain.KafkaRecoverySlaLog;
import com.safety.platform.repository.KafkaRecoverySlaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamsSlaServiceTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private KafkaRecoverySlaLogRepository slaLogRepository;

    @Mock
    private StreamsSlaProperties streamsSlaProperties;

    @InjectMocks
    private StreamsSlaService streamsSlaService;

    @BeforeEach
    void setUp() {
        lenient().when(streamsSlaProperties.isEnabled()).thenReturn(true);
        when(streamsSlaProperties.getThresholdMs()).thenReturn(30000L);
    }

    @Test
    void captureLatestRecoveryIfNeeded_savesSlaLogForNewDuration() {
        when(metricsService.getLastRecoveryDurationMs()).thenReturn(12000L);

        streamsSlaService.captureLatestRecoveryIfNeeded();

        ArgumentCaptor<KafkaRecoverySlaLog> captor = ArgumentCaptor.forClass(KafkaRecoverySlaLog.class);
        verify(slaLogRepository, times(1)).save(captor.capture());
        KafkaRecoverySlaLog saved = captor.getValue();
        assertThat(saved.getRecoveryDurationMs()).isEqualTo(12000L);
        assertThat(saved.getThresholdMs()).isEqualTo(30000L);
        assertThat(saved.isCompliant()).isTrue();
        assertThat(saved.getCheckedAt()).isNotNull();
    }

    @Test
    void captureLatestRecoveryIfNeeded_doesNotSaveDuplicateDuration() {
        when(metricsService.getLastRecoveryDurationMs()).thenReturn(25000L);

        streamsSlaService.captureLatestRecoveryIfNeeded();
        streamsSlaService.captureLatestRecoveryIfNeeded();

        verify(slaLogRepository, times(1)).save(org.mockito.ArgumentMatchers.any(KafkaRecoverySlaLog.class));
    }

    @Test
    void getStatus_returnsAggregatedSummary() {
        KafkaRecoverySlaLog latest = new KafkaRecoverySlaLog();
        latest.setRecoveryDurationMs(31000L);
        latest.setCompliant(false);

        when(slaLogRepository.count()).thenReturn(3L);
        when(slaLogRepository.countByCompliant(true)).thenReturn(2L);
        when(slaLogRepository.findTopByOrderByCheckedAtDesc()).thenReturn(Optional.of(latest));
        when(slaLogRepository.findTop20ByOrderByCheckedAtDesc()).thenReturn(List.of(latest));

        var status = streamsSlaService.getStatus();

        assertThat(status.getTotalChecks()).isEqualTo(3L);
        assertThat(status.getPassedChecks()).isEqualTo(2L);
        assertThat(status.getFailedChecks()).isEqualTo(1L);
        assertThat(status.getLatestRecoveryDurationMs()).isEqualTo(31000L);
        assertThat(status.getLatestCompliant()).isFalse();
        assertThat(status.getRecentChecks()).hasSize(1);
    }
}
