package com.safety.platform.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final Counter immutabilityViolationCounter;
    private final Counter outboxSuccessCounter;
    private final Counter outboxFailureCounter;
    private final Timer kafkaRecoveryTimer;
    private final AtomicLong lastRecoveryDurationMs = new AtomicLong(0L);
    private final AtomicInteger kafkaStreamsStateCode = new AtomicInteger(0);

    public MetricsService(MeterRegistry meterRegistry) {
        this.immutabilityViolationCounter = Counter.builder("safety_immutability_violation_total")
            .description("Count of blocked event modifications")
            .register(meterRegistry);
        this.outboxSuccessCounter = Counter.builder("safety_outbox_sent_total")
            .description("Outbox delivery success count")
            .register(meterRegistry);
        this.outboxFailureCounter = Counter.builder("safety_outbox_failed_total")
            .description("Outbox delivery failure count")
            .register(meterRegistry);
        this.kafkaRecoveryTimer = Timer.builder("kafka_streams_state_recovery_time")
            .description("Kafka streams recovery duration")
            .publishPercentileHistogram()
            .register(meterRegistry);
        Gauge.builder("kafka_streams_state_recovery_time_ms", lastRecoveryDurationMs, AtomicLong::get)
            .description("Last Kafka streams recovery duration in milliseconds")
            .register(meterRegistry);
        Gauge.builder("kafka_streams_state_code", kafkaStreamsStateCode, AtomicInteger::get)
            .description("Kafka streams state code (ERROR=-1, UNKNOWN=0, CREATED=1, REBALANCING=2, RUNNING=3)")
            .register(meterRegistry);
    }

    public void recordImmutabilityViolation() {
        immutabilityViolationCounter.increment();
    }

    public void recordOutboxSent() {
        outboxSuccessCounter.increment();
    }

    public void recordOutboxFailed() {
        outboxFailureCounter.increment();
    }

    public void recordRecoveryDurationMillis(long millis) {
        lastRecoveryDurationMs.set(millis);
        kafkaRecoveryTimer.record(Duration.ofMillis(millis));
    }

    public void recordStateChange(String state) {
        kafkaStreamsStateCode.set(mapStateToCode(state));
    }

    public long getLastRecoveryDurationMs() {
        return lastRecoveryDurationMs.get();
    }

    public int getKafkaStreamsStateCode() {
        return kafkaStreamsStateCode.get();
    }

    private int mapStateToCode(String state) {
        if (state == null) {
            return 0;
        }
        return switch (state) {
            case "ERROR" -> -1;
            case "CREATED" -> 1;
            case "REBALANCING" -> 2;
            case "RUNNING" -> 3;
            default -> 0;
        };
    }
}
