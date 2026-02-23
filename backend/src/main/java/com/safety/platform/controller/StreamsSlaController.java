package com.safety.platform.controller;

import com.safety.platform.dto.StreamsSlaStatusResponse;
import com.safety.platform.service.StreamsSlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streams")
@RequiredArgsConstructor
public class StreamsSlaController {

    private final StreamsSlaService streamsSlaService;

    @GetMapping("/sla")
    public StreamsSlaStatusResponse getSlaStatus() {
        return streamsSlaService.getStatus();
    }
}

