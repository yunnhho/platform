package com.safety.platform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic sensorReadingsTopic() {
        return TopicBuilder.name("sensor-readings")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic sensorAlertsTopic() {
        return TopicBuilder.name("sensor-alerts")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic sensorStatsTopic() {
        return TopicBuilder.name("sensor-stats")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
