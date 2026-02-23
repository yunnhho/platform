package com.safety.platform.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SensorAlertMessage {
    String sensorId;
    String alertType;
    String message;
    LocalDateTime occurredAt;
}
