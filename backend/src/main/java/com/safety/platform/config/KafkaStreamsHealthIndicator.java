package com.safety.platform.config;

import com.safety.platform.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component("kafkaStreams")
@RequiredArgsConstructor
@Slf4j
public class KafkaStreamsHealthIndicator implements HealthIndicator {

    private final ObjectProvider<StreamsBuilderFactoryBean> streamsFactoryBeanProvider;
    private final Environment environment;
    private final MetricsService metricsService;

    @Override
    public Health health() {
        StreamsBuilderFactoryBean streamsFactoryBean = streamsFactoryBeanProvider.getIfAvailable();
        if (streamsFactoryBean == null) {
            return Health.unknown().withDetail("state", "DISABLED").build();
        }
        KafkaStreams kafkaStreams = streamsFactoryBean.getKafkaStreams();
        if (kafkaStreams == null) {
            return Health.unknown().withDetail("state", "NOT_INITIALIZED").build();
        }

        KafkaStreams.State state = kafkaStreams.state();
        long stateStoreSize = calculateStateStoreSize();
        if (state == KafkaStreams.State.RUNNING || state == KafkaStreams.State.REBALANCING) {
            return Health.up()
                .withDetail("state", state.name())
                .withDetail("stateCode", metricsService.getKafkaStreamsStateCode())
                .withDetail("stateStoreSizeBytes", stateStoreSize)
                .withDetail("lastRecoveryTimeMs", metricsService.getLastRecoveryDurationMs())
                .build();
        }

        return Health.down()
            .withDetail("state", state.name())
            .withDetail("stateCode", metricsService.getKafkaStreamsStateCode())
            .withDetail("stateStoreSizeBytes", stateStoreSize)
            .build();
    }

    private long calculateStateStoreSize() {
        String stateDir = environment.getProperty("spring.kafka.streams.state-dir", "./kafka-streams-state");
        try {
            Path path = Path.of(stateDir);
            if (!Files.exists(path)) {
                return 0L;
            }
            return Files.walk(path)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        log.debug("Unable to read state file size: {}", p, e);
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            log.warn("Failed to compute kafka streams state store size", e);
            return 0L;
        }
    }
}
