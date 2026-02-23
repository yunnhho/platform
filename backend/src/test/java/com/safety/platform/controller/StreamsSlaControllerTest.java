package com.safety.platform.controller;

import com.safety.platform.dto.StreamsSlaStatusResponse;
import com.safety.platform.service.StreamsSlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StreamsSlaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StreamsSlaService streamsSlaService;

    @InjectMocks
    private StreamsSlaController streamsSlaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(streamsSlaController).build();
    }

    @Test
    void getSlaStatus_returnsSummary() throws Exception {
        StreamsSlaStatusResponse response = StreamsSlaStatusResponse.builder()
            .thresholdMs(30000L)
            .totalChecks(5L)
            .passedChecks(4L)
            .failedChecks(1L)
            .latestRecoveryDurationMs(12000L)
            .latestCompliant(true)
            .build();
        when(streamsSlaService.getStatus()).thenReturn(response);

        mockMvc.perform(get("/api/streams/sla"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.thresholdMs").value(30000))
            .andExpect(jsonPath("$.totalChecks").value(5))
            .andExpect(jsonPath("$.passedChecks").value(4))
            .andExpect(jsonPath("$.failedChecks").value(1))
            .andExpect(jsonPath("$.latestCompliant").value(true));
    }
}

