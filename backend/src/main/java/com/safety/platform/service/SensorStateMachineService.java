package com.safety.platform.service;

import com.safety.platform.domain.SensorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SensorStateMachineService {

    private final Map<String, SensorStatus> sensorState = new ConcurrentHashMap<>();

    public SensorStatus transition(String sensorId, SensorStatus next) {
        SensorStatus previous = sensorState.put(sensorId, next);
        if (previous != null && previous != next) {
            log.info("Sensor state transition. sensor={}, {} -> {}", sensorId, previous, next);
        }
        return previous;
    }
}
