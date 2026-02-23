package com.safety.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class KafkaLagResponse {
    private String consumerGroup;
    private long totalLag;
    private LocalDateTime checkedAt;
    private List<KafkaTopicLagResponse> topics;
}

