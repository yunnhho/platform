package com.safety.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StorageTierSummaryResponse {
    private long hotCount;
    private long warmCount;
    private long coldCount;
    private long archivedColdCount;
    private LocalDateTime hotBoundary;
    private LocalDateTime coldBoundary;
}

