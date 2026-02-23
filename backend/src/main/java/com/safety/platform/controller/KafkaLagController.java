package com.safety.platform.controller;

import com.safety.platform.dto.KafkaLagResponse;
import com.safety.platform.service.KafkaLagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaLagController {

    private final KafkaLagService kafkaLagService;

    @GetMapping("/lag")
    public KafkaLagResponse getLag(@RequestParam(defaultValue = "safety-group") String groupId) {
        return kafkaLagService.getLag(groupId);
    }
}

