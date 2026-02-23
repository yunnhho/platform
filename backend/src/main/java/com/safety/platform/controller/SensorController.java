package com.safety.platform.controller;

import com.safety.platform.dto.SensorData;
import com.safety.platform.service.SensorCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorCacheService sensorCacheService;

    @GetMapping("/latest")
    public List<SensorData> getLatestFromCache() {
        return sensorCacheService.getLatestReadings();
    }
}
