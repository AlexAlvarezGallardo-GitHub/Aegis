package com.aegis.fraud;

import com.aegis.fraud.infrastructure.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aegis Fraud Service application entry point.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class FraudServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudServiceApplication.class, args);
    }
}
