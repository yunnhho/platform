package com.safety.platform.service;

import com.safety.platform.dto.SensorAlertMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @CircuitBreaker(name = "alertService", fallbackMethod = "fallbackAlert")
    public void sendCriticalAlert(String type, String message) {
        SensorAlertMessage alert = SensorAlertMessage.builder()
            .sensorId("SYSTEM")
            .alertType(type)
            .message(message)
            .occurredAt(LocalDateTime.now())
            .build();
        kafkaTemplate.send("sensor-alerts", alert.getSensorId(), alert);
    }

    @SuppressWarnings("unused")
    public void fallbackAlert(String type, String message, Throwable throwable) {
        log.error("Failed to send alert. type={}, message={}", type, message, throwable);
    }
}
