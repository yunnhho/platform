package com.safety.platform.scheduler;

import com.safety.platform.dto.SensorData;
import com.safety.platform.service.EventService;
import com.safety.platform.service.MockScadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SensorPollingScheduler {

    private final MockScadaService mockScadaService;
    private final EventService eventService;

    @Scheduled(fixedRate = 1000)
    public void pollSensors() {
        log.debug("Polling sensor data...");
        try {
            List<SensorData> readings = mockScadaService.generateSensorReadings();
            
            for (SensorData data : readings) {
                eventService.saveSensorReading(data);
            }
            
            log.info("Successfully polled and saved {} sensor readings", readings.size());
            
        } catch (Exception e) {
            log.error("Failed to poll or save sensor data", e);
        }
    }
}
