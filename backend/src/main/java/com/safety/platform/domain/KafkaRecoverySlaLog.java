package com.safety.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kafka_recovery_sla_log")
@Getter
@Setter
@NoArgsConstructor
public class KafkaRecoverySlaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    @Column(nullable = false)
    private long recoveryDurationMs;

    @Column(nullable = false)
    private long thresholdMs;

    @Column(nullable = false)
    private boolean compliant;
}

