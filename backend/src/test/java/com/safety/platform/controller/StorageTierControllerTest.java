package com.safety.platform.controller;

import com.safety.platform.dto.StorageArchiveRunResponse;
import com.safety.platform.dto.StorageTierSummaryResponse;
import com.safety.platform.service.StorageTierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StorageTierControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StorageTierService storageTierService;

    @InjectMocks
    private StorageTierController storageTierController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storageTierController).build();
    }

    @Test
    void getSummary_returnsTierCounts() throws Exception {
        StorageTierSummaryResponse response = StorageTierSummaryResponse.builder()
            .hotCount(10)
            .warmCount(20)
            .coldCount(30)
            .archivedColdCount(15)
            .build();
        when(storageTierService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/api/storage/tiers/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hotCount").value(10))
            .andExpect(jsonPath("$.warmCount").value(20))
            .andExpect(jsonPath("$.coldCount").value(30))
            .andExpect(jsonPath("$.archivedColdCount").value(15));
    }

    @Test
    void runArchive_returnsExecutionResult() throws Exception {
        StorageArchiveRunResponse response = StorageArchiveRunResponse.builder()
            .success(true)
            .archivedCount(12)
            .objectKey("cold/2026/02/23/events-20260223-123000-12.jsonl")
            .message("Archived cold events to object storage.")
            .executedAt(LocalDateTime.of(2026, 2, 23, 12, 30))
            .build();
        when(storageTierService.archiveColdEvents()).thenReturn(response);

        mockMvc.perform(post("/api/storage/tiers/archive/run"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.archivedCount").value(12))
            .andExpect(jsonPath("$.objectKey").exists());
    }
}

