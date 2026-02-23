package com.safety.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safety.platform.domain.SensorStatus;
import com.safety.platform.dto.SensorAlertMessage;
import com.safety.platform.dto.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final SensorCacheService sensorCacheService;
    private final SensorStateMachineService stateMachineService;
    private final OutboxService outboxService;
    private final SensorBroadcastService sensorBroadcastService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sensor-readings", groupId = "safety-group")
    public void consumeSensorReading(SensorData data) {
        sensorCacheService.cacheLatest(data);
        SensorStatus previous = stateMachineService.transition(data.getSensorId(), data.getStatus());
        sensorBroadcastService.broadcast(data);

        if (data.getStatus() == SensorStatus.CRITICAL && previous != SensorStatus.CRITICAL) {
            SensorAlertMessage alert = SensorAlertMessage.builder()
                .sensorId(data.getSensorId())
                .alertType("SENSOR_CRITICAL")
                .message("Critical threshold exceeded: " + data.getValue())
                .occurredAt(LocalDateTime.now())
                .build();
            try {
                outboxService.enqueue(data.getSensorId(), "SENSOR_CRITICAL", objectMapper.writeValueAsString(alert));
            } catch (Exception e) {
                log.error("Failed to enqueue outbox alert", e);
            }
        }
    }
}
