package com.safety.platform.service;

import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.repository.SafetyEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIntegrityValidator {

    private final SafetyEventLogRepository eventRepository;
    private final AlertService alertService;

    @Scheduled(cron = "0 0 3 * * *")
    public void validateEventChain() {
        List<String> aggregateIds = eventRepository.findDistinctAggregateIds();
        for (String aggregateId : aggregateIds) {
            if (!validateChain(aggregateId)) {
                alertService.sendCriticalAlert(
                    "DATA_INTEGRITY_VIOLATION",
                    "Event chain broken for sensor: " + aggregateId
                );
            }
        }
    }

    public boolean validateChain(String aggregateId) {
        List<SafetyEventLog> events = eventRepository.findByAggregateIdOrderByOccurredAtAsc(aggregateId);
        String expectedHash = "GENESIS";
        for (SafetyEventLog event : events) {
            if (!expectedHash.equals(event.getPreviousHash())) {
                log.error("Hash chain broken at event {}", event.getEventId());
                return false;
            }
            String recalculated = calculateHash(event);
            if (!recalculated.equals(event.getCurrentHash())) {
                log.error("Hash mismatch at event {}", event.getEventId());
                return false;
            }
            expectedHash = event.getCurrentHash();
        }
        return true;
    }

    private String calculateHash(SafetyEventLog event) {
        String data = event.getAggregateId()
            + event.getEventType()
            + event.getOccurredAt()
            + event.getPayload()
            + event.getPreviousHash();
        return DigestUtils.sha256Hex(data);
    }
}
