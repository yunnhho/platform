package com.safety.platform.service;

import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.domain.SensorStatus;
import com.safety.platform.dto.SensorData;
import com.safety.platform.repository.SafetyEventLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "app.scheduler.sensor-polling.enabled=false",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.streams.auto-startup=false"
})
class EventServiceIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private SafetyEventLogRepository eventRepository;

    @Test
    void saveSensorReading_persistsEventLog() {
        SensorData data = SensorData.builder()
            .sensorId("SENSOR_01_" + System.nanoTime())
            .value(73.5)
            .timestamp(LocalDateTime.now())
            .status(SensorStatus.NORMAL)
            .build();

        assertThat(eventRepository.findTopByAggregateIdOrderByOccurredAtDesc(data.getSensorId())).isEmpty();

        eventService.saveSensorReading(data);

        SafetyEventLog saved = eventRepository.findTopByAggregateIdOrderByOccurredAtDesc(data.getSensorId())
            .orElseThrow();
        assertThat(saved.getAggregateId()).isEqualTo(data.getSensorId());
        assertThat(saved.getPayload()).contains("73.5");
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getPreviousHash()).isNotBlank();
        assertThat(saved.getCurrentHash()).isNotBlank();
    }

    @Test
    void updateAttempt_throwsByPreUpdateGuard() {
        SensorData data = SensorData.builder()
            .sensorId("SENSOR_02_" + System.nanoTime())
            .value(95.2)
            .timestamp(LocalDateTime.now())
            .status(SensorStatus.CRITICAL)
            .build();

        eventService.saveSensorReading(data);
        SafetyEventLog saved = eventRepository.findAll().getFirst();
        saved.setPayload("{\"tampered\":true}");

        assertThatThrownBy(() -> eventRepository.saveAndFlush(saved))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("Event log cannot be modified");
    }
}
