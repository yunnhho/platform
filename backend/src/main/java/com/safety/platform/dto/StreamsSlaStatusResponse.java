package com.safety.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StreamsSlaStatusResponse {
    private long thresholdMs;
    private long totalChecks;
    private long passedChecks;
    private long failedChecks;
    private Long latestRecoveryDurationMs;
    private Boolean latestCompliant;
    private LocalDateTime latestCheckedAt;
    private List<StreamsSlaCheckItemResponse> recentChecks;
}

