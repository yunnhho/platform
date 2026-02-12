package com.safety.platform.service;

import com.safety.platform.domain.SensorStatus;
import com.safety.platform.dto.SensorData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockScadaService {

    private final Random random = new Random();

    public List<SensorData> generateSensorReadings() {
        List<SensorData> readings = new ArrayList<>();
        
        for (int i = 1; i <= 4; i++) {
            double value = 20 + (random.nextDouble() * 80); // 20 ~ 100
            SensorStatus status = determineStatus(value);
            
            readings.add(SensorData.builder()
                .sensorId("SENSOR_0" + i)
                .value(Math.round(value * 100.0) / 100.0) // 소수점 2자리
                .timestamp(LocalDateTime.now())
                .status(status)
                .build());
        }
        
        return readings;
    }

    private SensorStatus determineStatus(double value) {
        if (value > 90) return SensorStatus.CRITICAL;
        if (value > 75) return SensorStatus.WARNING;
        return SensorStatus.NORMAL;
    }
}
