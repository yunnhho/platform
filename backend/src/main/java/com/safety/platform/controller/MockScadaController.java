package com.safety.platform.controller;

import com.safety.platform.dto.SensorData;
import com.safety.platform.service.MockScadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mock/scada")
@RequiredArgsConstructor
public class MockScadaController {

    private final MockScadaService mockScadaService;

    @GetMapping("/sensors")
    public List<SensorData> getSensorReadings() {
        return mockScadaService.generateSensorReadings();
    }
}
