package com.safety.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safety.platform.domain.EventType;
import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.dto.SensorData;
import com.safety.platform.repository.SafetyEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final SafetyEventLogRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveSensorReading(SensorData data) {
        try {
            SafetyEventLog event = new SafetyEventLog();
            event.setAggregateId(data.getSensorId());
            event.setEventType(EventType.SENSOR_READING);
            event.setSourceSystem("SCADA_MOCK");
            event.setPayload(objectMapper.writeValueAsString(data));
            
            // occurredAt is set by @CreatedDate
            // audit fields set automatically if Auditing is enabled, but we need manual handling for now or assume @CreatedDate works
            
            eventRepository.save(event);
            log.debug("Saved event for sensor: {}", data.getSensorId());
            
        } catch (Exception e) {
            log.error("Failed to save sensor reading", e);
            throw new RuntimeException("Event save failed", e);
        }
    }
}
