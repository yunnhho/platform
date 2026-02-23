package com.safety.platform.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KafkaTopicLagResponse {
    private String topic;
    private int partitionCount;
    private long committedOffset;
    private long endOffset;
    private long lag;
}

