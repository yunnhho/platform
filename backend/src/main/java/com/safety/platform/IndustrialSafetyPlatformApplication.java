package com.safety.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@EnableScheduling
@EnableJpaAuditing
public class IndustrialSafetyPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(IndustrialSafetyPlatformApplication.class, args);
	}

}
