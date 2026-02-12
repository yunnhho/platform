package com.safety.platform.dto;

import com.safety.platform.domain.SensorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    private String sensorId;
    private double value;
    private LocalDateTime timestamp;
    private SensorStatus status;
}
