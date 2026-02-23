package com.safety.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StorageArchiveRunResponse {
    private boolean success;
    private int archivedCount;
    private String objectKey;
    private String message;
    private LocalDateTime executedAt;
}

