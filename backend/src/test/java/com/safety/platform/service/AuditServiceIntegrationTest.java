package com.safety.platform.service;

import com.safety.platform.dto.AuditStatsResponse;
import com.safety.platform.repository.EventAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.scheduler.sensor-polling.enabled=false",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.streams.auto-startup=false"
})
class AuditServiceIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private EventAuditLogRepository eventAuditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        eventAuditLogRepository.deleteAll();
    }

    @Test
    void getStatistics_returnsOperationCounts() {
        jdbcTemplate.update(
            "INSERT INTO event_audit_log (operation, event_id, attempted_by, denied_reason) VALUES (?, ?, ?, ?)",
            "UPDATE", UUID.randomUUID(), "tester", "blocked"
        );
        jdbcTemplate.update(
            "INSERT INTO event_audit_log (operation, event_id, attempted_by, denied_reason) VALUES (?, ?, ?, ?)",
            "DELETE", UUID.randomUUID(), "tester", "blocked"
        );

        AuditStatsResponse stats = auditService.getStatistics();

        assertThat(stats.getTotalAttempts()).isEqualTo(2);
        assertThat(stats.getUpdateAttempts()).isEqualTo(1);
        assertThat(stats.getDeleteAttempts()).isEqualTo(1);
        assertThat(stats.getLastAttemptedAt()).isNotNull();
    }
}
