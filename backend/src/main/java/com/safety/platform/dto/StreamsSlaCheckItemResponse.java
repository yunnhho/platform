package com.safety.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StreamsSlaCheckItemResponse {
    private LocalDateTime checkedAt;
    private long recoveryDurationMs;
    private long thresholdMs;
    private boolean compliant;
}

