package com.safety.platform.service;

import com.safety.platform.dto.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(SensorData data) {
        messagingTemplate.convertAndSend("/topic/sensors", data);
    }
}
