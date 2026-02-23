package com.safety.platform.controller;

import com.safety.platform.service.OutboxService;
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
class OutboxControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OutboxController outboxController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(outboxController).build();
    }

    @Test
    void getStats_returnsPendingCount() throws Exception {
        when(outboxService.getPendingCount()).thenReturn(7L);

        mockMvc.perform(get("/api/outbox/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pending").value(7));
    }
}
