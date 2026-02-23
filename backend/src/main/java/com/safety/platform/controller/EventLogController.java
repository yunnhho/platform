package com.safety.platform.controller;

import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventLogController {

    private final EventService eventService;

    @GetMapping("/recent")
    public List<SafetyEventLog> getRecentEvents() {
        return eventService.getRecentEvents();
    }
}
