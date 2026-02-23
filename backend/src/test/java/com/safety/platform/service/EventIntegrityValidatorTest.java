package com.safety.platform.service;

import com.safety.platform.domain.EventType;
import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.repository.SafetyEventLogRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIntegrityValidatorTest {

    @Mock
    private SafetyEventLogRepository eventRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private EventIntegrityValidator validator;

    @Test
    void validateChain_returnsTrue_whenChainIsValid() {
        String aggregateId = "SENSOR_VALID";
        LocalDateTime baseTime = LocalDateTime.of(2026, 2, 20, 10, 0);
        SafetyEventLog first = createEvent(
            aggregateId,
            baseTime,
            "{\"value\":55.0}",
            "GENESIS"
        );
        SafetyEventLog second = createEvent(
            aggregateId,
            baseTime.plusSeconds(1),
            "{\"value\":60.0}",
            first.getCurrentHash()
        );

        when(eventRepository.findByAggregateIdOrderByOccurredAtAsc(aggregateId))
            .thenReturn(List.of(first, second));

        assertThat(validator.validateChain(aggregateId)).isTrue();
    }

    @Test
    void validateChain_returnsFalse_whenPreviousHashIsBroken() {
        String aggregateId = "SENSOR_BROKEN";
        LocalDateTime baseTime = LocalDateTime.of(2026, 2, 20, 11, 0);
        SafetyEventLog first = createEvent(
            aggregateId,
            baseTime,
            "{\"value\":65.0}",
            "GENESIS"
        );
        SafetyEventLog second = createEvent(
            aggregateId,
            baseTime.plusSeconds(1),
            "{\"value\":75.0}",
            "WRONG_HASH"
        );

        when(eventRepository.findByAggregateIdOrderByOccurredAtAsc(aggregateId))
            .thenReturn(List.of(first, second));

        assertThat(validator.validateChain(aggregateId)).isFalse();
    }

    private SafetyEventLog createEvent(
        String aggregateId,
        LocalDateTime occurredAt,
        String payload,
        String previousHash
    ) {
        SafetyEventLog event = new SafetyEventLog();
        event.setEventId(UUID.randomUUID());
        event.setAggregateId(aggregateId);
        event.setEventType(EventType.SENSOR_READING);
        event.setOccurredAt(occurredAt);
        event.setPayload(payload);
        event.setSourceSystem("SCADA_MOCK");
        event.setPreviousHash(previousHash);
        event.setCurrentHash(calculateHash(event));
        return event;
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
