package com.aegis.bff.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties for the BFF service.
 *
 * <p>Bound from the {@code aegis.*} namespace in application.yml.</p>
 *
 * @param identityService identity-service connection settings
 * @param walletService   wallet-service connection settings
 * @param jwt             JWT validation settings
 */
@ConfigurationProperties(prefix = "aegis")
public record BffProperties(ServiceUrl identityService, ServiceUrl walletService, Jwt jwt) {

    public record ServiceUrl(String url) {
    }

    public record Jwt(String secret) {
    }
}
