package com.aegis.bff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class BffService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SessionJwtStore sessionJwtStore;

    public BffService(
            @Value("${aegis.identity-service.url}") String identityServiceUrl,
            ObjectMapper objectMapper,
            SessionJwtStore sessionJwtStore) {
        this.webClient = WebClient.builder()
                .baseUrl(identityServiceUrl)
                .build();
        this.objectMapper = objectMapper;
        this.sessionJwtStore = sessionJwtStore;
    }

    public Mono<Map<String, Object>> login(String email, String password, String correlationId) {
        return webClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Correlation-Id", correlationId)
                .bodyValue(Map.of("email", email, "password", password))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    String accessToken = response.get("accessToken").asText();
                    String refreshToken = response.get("refreshToken").asText();
                    sessionJwtStore.storeTokens(accessToken, refreshToken);

                    return Map.<String, Object>of(
                            "tokenType", "Bearer",
                            "expiresIn", response.get("expiresIn").asLong(),
                            "emailVerified", response.get("emailVerified").asBoolean()
                    );
                });
    }

    public Mono<Map<String, Object>> refresh(String correlationId) {
        String refreshToken = sessionJwtStore.getRefreshToken()
                .orElseThrow(() -> new RuntimeException("No refresh token in session"));

        return webClient.post()
                .uri("/api/v1/auth/refresh")
                .header("X-Correlation-Id", correlationId)
                .bodyValue(Map.of("refreshToken", refreshToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    String newAccessToken = response.get("accessToken").asText();
                    String newRefreshToken = response.get("refreshToken").asText();
                    sessionJwtStore.storeTokens(newAccessToken, newRefreshToken);

                    return Map.<String, Object>of(
                            "tokenType", "Bearer",
                            "expiresIn", response.get("expiresIn").asLong()
                    );
                });
    }

    public void logout() {
        sessionJwtStore.clear();
    }

    public Map<String, Object> getCurrentUser() {
        String accessToken = sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));

        var parts = accessToken.split("\\.");
        if (parts.length < 2) throw new RuntimeException("Invalid token");

        try {
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            return Map.of(
                    "userId", claims.get("sub").asText(),
                    "email", claims.get("email").asText()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode token", e);
        }
    }
}
