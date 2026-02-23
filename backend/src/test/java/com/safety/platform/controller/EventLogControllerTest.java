package com.safety.platform.controller;

import com.safety.platform.domain.EventType;
import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventLogController eventLogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventLogController).build();
    }

    @Test
    void getRecentEvents_returnsEvents() throws Exception {
        SafetyEventLog event = new SafetyEventLog();
        event.setEventId(UUID.randomUUID());
        event.setAggregateId("SENSOR_01");
        event.setEventType(EventType.SENSOR_READING);
        event.setOccurredAt(LocalDateTime.of(2026, 2, 20, 12, 0));
        event.setPayload("{\"value\":73.5}");
        event.setSourceSystem("SCADA_MOCK");
        event.setPreviousHash("GENESIS");
        event.setCurrentHash("hash-1");

        when(eventService.getRecentEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].aggregateId").value("SENSOR_01"))
            .andExpect(jsonPath("$[0].eventType").value("SENSOR_READING"))
            .andExpect(jsonPath("$[0].payload").value("{\"value\":73.5}"));
    }
}
