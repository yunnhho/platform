package com.safety.platform.service;

import com.safety.platform.domain.OutboxMessage;
import com.safety.platform.domain.OutboxStatus;
import com.safety.platform.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MetricsService metricsService;

    @Transactional
    public void enqueue(String aggregateId, String eventType, String payload) {
        OutboxMessage message = new OutboxMessage();
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        message.setPayload(payload);
        message.setStatus(OutboxStatus.PENDING);
        outboxMessageRepository.save(message);
    }

    @Transactional
    public void processPending() {
        List<OutboxMessage> messages = outboxMessageRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PENDING,
            PageRequest.of(0, 100)
        );
        for (OutboxMessage message : messages) {
            try {
                kafkaTemplate.send("sensor-alerts", message.getAggregateId(), message.getPayload());
                message.setStatus(OutboxStatus.SENT);
                message.setProcessedAt(LocalDateTime.now());
                metricsService.recordOutboxSent();
            } catch (Exception e) {
                message.setStatus(OutboxStatus.FAILED);
                message.setRetryCount(message.getRetryCount() + 1);
                metricsService.recordOutboxFailed();
                log.error("Outbox delivery failed. id={}", message.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return outboxMessageRepository.countByStatus(OutboxStatus.PENDING);
    }
}
