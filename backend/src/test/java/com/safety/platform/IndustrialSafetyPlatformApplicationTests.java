package com.safety.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "app.scheduler.sensor-polling.enabled=false",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.streams.auto-startup=false"
})
class IndustrialSafetyPlatformApplicationTests {

	@Test
	void contextLoads() {
	}

}
