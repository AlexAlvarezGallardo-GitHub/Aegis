package com.aegis.bff.infrastructure.client;

import com.aegis.bff.domain.port.PaymentClient;
import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RestClient-based implementation of {@link PaymentClient}.
 */
@Component
public class RestPaymentClient implements PaymentClient {

    private final RestClient restClient;

    public RestPaymentClient(RestClient.Builder restClientBuilder, BffProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.paymentService().url()).build();
    }

    @Override
    public JsonNode transferFunds(String accessToken, String userId,
                                  String sourceWalletId, String destWalletId,
                                  BigDecimal amount, String currency,
                                  String description, String reference,
                                  String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceWalletId", sourceWalletId);
        body.put("destWalletId", destWalletId);
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("currency", currency);
        if (description != null) {
            body.put("description", description);
        }
        body.put("reference", reference);

        return restClient.post()
                .uri("/api/v1/transfers")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode getTransfer(String accessToken, String userId,
                                String transferId, String correlationId) {
        return restClient.get()
                .uri("/api/v1/transfers/{transferId}", transferId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .retrieve()
                .body(JsonNode.class);
    }
}
