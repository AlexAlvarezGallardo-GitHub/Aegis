package com.aegis.bff.infrastructure.client;

import com.aegis.bff.domain.port.IdentityClient;
import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * RestClient-based implementation of {@link IdentityClient}.
 */
@Component
public class RestIdentityClient implements IdentityClient {

    private final RestClient restClient;

    public RestIdentityClient(RestClient.Builder restClientBuilder, BffProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.identityService().url()).build();
    }

    @Override
    public JsonNode login(String email, String password, String correlationId) {
        return restClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Correlation-Id", correlationId)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode refresh(String refreshToken, String correlationId) {
        return restClient.post()
                .uri("/api/v1/auth/refresh")
                .header("X-Correlation-Id", correlationId)
                .body(Map.of("refreshToken", refreshToken))
                .retrieve()
                .body(JsonNode.class);
    }
}
