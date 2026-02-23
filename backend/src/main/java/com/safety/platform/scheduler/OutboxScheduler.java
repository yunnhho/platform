package com.safety.platform.scheduler;

import com.safety.platform.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 3000)
    public void processOutbox() {
        outboxService.processPending();
    }
}
