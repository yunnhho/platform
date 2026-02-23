package com.safety.platform.config;

import com.safety.platform.service.AlertService;
import com.safety.platform.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.streams.auto-startup", havingValue = "true", matchIfMissing = true)
public class KafkaStreamsLifecycleConfig implements BeanPostProcessor {

    private final MetricsService metricsService;
    private final AlertService alertService;
    private final AtomicLong rebalanceStartedAtMs = new AtomicLong(-1L);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof StreamsBuilderFactoryBean factoryBean) {
            factoryBean.setStateListener((newState, oldState) -> {
                log.info("Kafka Streams state transition: {} -> {}", oldState, newState);
                metricsService.recordStateChange(newState.name());

                if (newState == KafkaStreams.State.REBALANCING) {
                    rebalanceStartedAtMs.set(System.currentTimeMillis());
                }

                if (newState == KafkaStreams.State.RUNNING) {
                    long startedAt = rebalanceStartedAtMs.getAndSet(-1L);
                    if (startedAt > 0) {
                        long durationMs = System.currentTimeMillis() - startedAt;
                        metricsService.recordRecoveryDurationMillis(durationMs);
                        log.info("Kafka Streams state recovery completed in {} ms", durationMs);
                    }
                }

                if (newState == KafkaStreams.State.ERROR) {
                    alertService.sendCriticalAlert("KAFKA_STREAMS_ERROR", "Kafka Streams entered ERROR state");
                }
            });
            factoryBean.setStreamsUncaughtExceptionHandler(exception -> {
                log.error("Kafka Streams uncaught exception", exception);
                alertService.sendCriticalAlert("KAFKA_STREAMS_EXCEPTION", exception.getMessage());
                return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
            });
        }
        return bean;
    }
}
