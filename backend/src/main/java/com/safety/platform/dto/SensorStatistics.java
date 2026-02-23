package com.safety.platform.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SensorStatistics {
    private long count;
    private double sum;
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;
    private double average;
    private String trend = "STABLE";

    public void addReading(double value) {
        count++;
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
        average = sum / count;
        trend = average > 80 ? "UP" : "STABLE";
    }
}
