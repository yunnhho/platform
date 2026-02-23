package com.safety.platform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AuditStatsResponse {
    long totalAttempts;
    long updateAttempts;
    long deleteAttempts;
    LocalDateTime lastAttemptedAt;
}
