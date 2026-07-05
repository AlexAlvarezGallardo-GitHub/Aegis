package com.aegis.bff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class BffService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SessionJwtStore sessionJwtStore;

    public BffService(
            @Value("${aegis.identity-service.url}") String identityServiceUrl,
            ObjectMapper objectMapper,
            SessionJwtStore sessionJwtStore) {
        this(RestClient.builder().baseUrl(identityServiceUrl).build(), objectMapper, sessionJwtStore);
    }

    BffService(RestClient restClient, ObjectMapper objectMapper, SessionJwtStore sessionJwtStore) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.sessionJwtStore = sessionJwtStore;
    }

    public Map<String, Object> login(String email, String password, String correlationId) {
        JsonNode response = restClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Correlation-Id", correlationId)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(JsonNode.class);

        String accessToken = response.get("accessToken").asText();
        String refreshToken = response.get("refreshToken").asText();
        sessionJwtStore.storeTokens(accessToken, refreshToken);

        return Map.of(
                "tokenType", "Bearer",
                "expiresIn", response.get("expiresIn").asLong(),
                "emailVerified", response.get("emailVerified").asBoolean()
        );
    }

    public Map<String, Object> refresh(String correlationId) {
        String refreshToken = sessionJwtStore.getRefreshToken()
                .orElseThrow(() -> new RuntimeException("No refresh token in session"));

        JsonNode response = restClient.post()
                .uri("/api/v1/auth/refresh")
                .header("X-Correlation-Id", correlationId)
                .body(Map.of("refreshToken", refreshToken))
                .retrieve()
                .body(JsonNode.class);

        String newAccessToken = response.get("accessToken").asText();
        String newRefreshToken = response.get("refreshToken").asText();
        sessionJwtStore.storeTokens(newAccessToken, newRefreshToken);

        return Map.of(
                "tokenType", "Bearer",
                "expiresIn", response.get("expiresIn").asLong()
        );
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
