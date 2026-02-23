package com.safety.platform.controller;

import com.safety.platform.dto.KafkaLagResponse;
import com.safety.platform.dto.KafkaTopicLagResponse;
import com.safety.platform.service.KafkaLagService;
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
class KafkaLagControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KafkaLagService kafkaLagService;

    @InjectMocks
    private KafkaLagController kafkaLagController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(kafkaLagController).build();
    }

    @Test
    void getLag_returnsLagPayload() throws Exception {
        KafkaLagResponse response = KafkaLagResponse.builder()
            .consumerGroup("safety-group")
            .totalLag(12L)
            .topics(List.of(
                KafkaTopicLagResponse.builder()
                    .topic("sensor-readings")
                    .partitionCount(3)
                    .committedOffset(100L)
                    .endOffset(112L)
                    .lag(12L)
                    .build()
            ))
            .build();
        when(kafkaLagService.getLag("safety-group")).thenReturn(response);

        mockMvc.perform(get("/api/kafka/lag?groupId=safety-group"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consumerGroup").value("safety-group"))
            .andExpect(jsonPath("$.totalLag").value(12))
            .andExpect(jsonPath("$.topics[0].topic").value("sensor-readings"))
            .andExpect(jsonPath("$.topics[0].lag").value(12));
    }
}

