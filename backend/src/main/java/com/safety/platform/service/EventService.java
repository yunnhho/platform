package com.safety.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safety.platform.domain.EventType;
import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.dto.SensorData;
import com.safety.platform.repository.SafetyEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final SafetyEventLogRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public void saveSensorReading(SensorData data) {
        try {
            SafetyEventLog event = new SafetyEventLog();
            event.setAggregateId(data.getSensorId());
            event.setEventType(EventType.SENSOR_READING);
            event.setSourceSystem("SCADA_MOCK");
            event.setPayload(objectMapper.writeValueAsString(data));
            event.setOccurredAt(LocalDateTime.now());
            applyHashChain(event);

            eventRepository.save(event);
            try {
                kafkaProducerService.publishSensorReading(data);
            } catch (Exception kafkaEx) {
                log.warn("Sensor reading saved but Kafka publish failed. sensor={}", data.getSensorId(), kafkaEx);
            }
            log.debug("Saved event for sensor: {}", data.getSensorId());
            
        } catch (Exception e) {
            log.error("Failed to save sensor reading", e);
            throw new RuntimeException("Event save failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<SafetyEventLog> getRecentEvents() {
        return eventRepository.findTop10ByOrderByOccurredAtDesc();
    }

    private void applyHashChain(SafetyEventLog event) {
        String previousHash = eventRepository.findTopByAggregateIdOrderByOccurredAtDesc(event.getAggregateId())
            .map(SafetyEventLog::getCurrentHash)
            .orElse("GENESIS");
        event.setPreviousHash(previousHash);
        String payload = event.getAggregateId()
            + event.getEventType()
            + event.getOccurredAt()
            + event.getPayload()
            + previousHash;
        event.setCurrentHash(DigestUtils.sha256Hex(payload));
    }
}
