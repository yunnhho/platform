package com.safety.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safety.platform.dto.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorCacheService {

    private static final String KEY_PREFIX = "sensor:latest:";
    private static final List<String> MANAGED_SENSOR_IDS = Arrays.asList(
        "SENSOR_01",
        "SENSOR_02",
        "SENSOR_03",
        "SENSOR_04"
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheLatest(SensorData data) {
        try {
            String key = KEY_PREFIX + data.getSensorId();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), 5, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache sensor data for {}", data.getSensorId(), e);
        }
    }

    public List<SensorData> getLatestReadings() {
        List<SensorData> readings = new ArrayList<>();
        for (String sensorId : MANAGED_SENSOR_IDS) {
            String key = KEY_PREFIX + sensorId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                continue;
            }
            try {
                readings.add(objectMapper.readValue(json, SensorData.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached sensor payload for key {}", key, e);
            }
        }
        return readings;
    }
}
