package com.safety.platform.controller;

import com.safety.platform.domain.EventAuditLog;
import com.safety.platform.dto.AuditStatsResponse;
import com.safety.platform.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditLogController auditLogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditLogController).build();
    }

    @Test
    void getAuditStats_returnsStatsPayload() throws Exception {
        AuditStatsResponse response = AuditStatsResponse.builder()
            .totalAttempts(4)
            .updateAttempts(3)
            .deleteAttempts(1)
            .lastAttemptedAt(null)
            .build();
        when(auditService.getStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/audit/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAttempts").value(4))
            .andExpect(jsonPath("$.updateAttempts").value(3))
            .andExpect(jsonPath("$.deleteAttempts").value(1));
    }

    @Test
    void getModificationAttempts_returnsPagedPayload() throws Exception {
        EventAuditLog log = new EventAuditLog();
        log.setAuditId(1L);
        log.setOperation("UPDATE");
        log.setEventId(UUID.fromString("c0f11590-fdd1-4f7f-ab58-cf2da9f08188"));
        log.setAttemptedBy("tester");
        log.setAttemptedAt(null);
        log.setDeniedReason("blocked");

        when(auditService.getModificationAttempts(any()))
            .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/audit/modification-attempts?page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].operation").value("UPDATE"))
            .andExpect(jsonPath("$.content[0].attemptedBy").value("tester"))
            .andExpect(jsonPath("$.content[0].deniedReason").value("blocked"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
