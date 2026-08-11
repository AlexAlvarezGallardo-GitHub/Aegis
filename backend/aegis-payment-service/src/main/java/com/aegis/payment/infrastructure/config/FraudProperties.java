package com.aegis.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the fraud-service integration.
 *
 * @param baseUrl base URL of the fraud service (e.g. {@code http://localhost:8089})
 * @param timeoutMs read timeout in milliseconds for fraud assessment calls
 */
@ConfigurationProperties(prefix = "aegis.payment.fraud")
public record FraudProperties(
        @DefaultValue("http://localhost:8089") String baseUrl,
        @DefaultValue("3000") int timeoutMs
) {}
