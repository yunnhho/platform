package com.safety.platform.controller;

import com.safety.platform.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxService outboxService;

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return Map.of("pending", outboxService.getPendingCount());
    }
}
