package com.safety.platform.controller;

import com.safety.platform.domain.SensorStatus;
import com.safety.platform.dto.SensorData;
import com.safety.platform.service.SensorCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SensorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SensorCacheService sensorCacheService;

    @InjectMocks
    private SensorController sensorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sensorController).build();
    }

    @Test
    void getLatestFromCache_returnsReadings() throws Exception {
        SensorData data = SensorData.builder()
            .sensorId("SENSOR_11")
            .value(81.2)
            .timestamp(null)
            .status(SensorStatus.WARNING)
            .build();
        when(sensorCacheService.getLatestReadings()).thenReturn(List.of(data));

        mockMvc.perform(get("/api/sensors/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sensorId").value("SENSOR_11"))
            .andExpect(jsonPath("$[0].value").value(81.2))
            .andExpect(jsonPath("$[0].status").value("WARNING"));
    }
}
