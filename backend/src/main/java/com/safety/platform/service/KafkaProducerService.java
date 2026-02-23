package com.safety.platform.service;

import com.safety.platform.dto.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSensorReading(SensorData data) {
        kafkaTemplate.send("sensor-readings", data.getSensorId(), data);
    }
}
