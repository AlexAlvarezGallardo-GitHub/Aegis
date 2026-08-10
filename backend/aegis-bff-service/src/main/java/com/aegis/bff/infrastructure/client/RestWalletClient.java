package com.aegis.bff.infrastructure.client;

import com.aegis.bff.domain.port.WalletClient;
import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RestClient-based implementation of {@link WalletClient}.
 */
@Component
public class RestWalletClient implements WalletClient {

    private final RestClient restClient;

    public RestWalletClient(RestClient.Builder restClientBuilder, BffProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.walletService().url()).build();
    }

    @Override
    public JsonNode createWallet(String accessToken, String userId, String currency,
                                 String correlationId) {
        return restClient.post()
                .uri("/api/v1/wallets")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .body(Map.of("currency", currency))
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode listWallets(String accessToken, String userId) {
        return restClient.get()
                .uri("/api/v1/wallets")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode getWallet(String accessToken, String userId, String walletId) {
        return restClient.get()
                .uri("/api/v1/wallets/{walletId}", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode adjustBalance(String accessToken, String userId, String walletId,
                                  BigDecimal amount, String description,
                                  String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        if (description != null) {
            body.put("description", description);
        }

        return restClient.patch()
                .uri("/api/v1/wallets/{walletId}/balance", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode depositFunds(String accessToken, String userId, String walletId,
                                 BigDecimal amount, String currency, String source,
                                 String reference, String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("source", source);
        if (reference != null) {
            body.put("reference", reference);
        }

        return restClient.post()
                .uri("/api/v1/wallets/{walletId}/deposits", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode updateStatus(String accessToken, String userId, String walletId,
                                 String status) {
        return restClient.patch()
                .uri("/api/v1/wallets/{walletId}/status", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .body(Map.of("status", status))
                .retrieve()
                .body(JsonNode.class);
    }
}
