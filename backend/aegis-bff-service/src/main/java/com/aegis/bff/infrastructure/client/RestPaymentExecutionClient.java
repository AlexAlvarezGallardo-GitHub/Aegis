package com.aegis.bff.infrastructure.client;

import com.aegis.bff.domain.port.PaymentExecutionClient;
import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RestClient-based implementation of {@link PaymentExecutionClient}.
 */
@Component
public class RestPaymentExecutionClient implements PaymentExecutionClient {

    private final RestClient restClient;

    public RestPaymentExecutionClient(RestClient.Builder restClientBuilder, BffProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.paymentService().url()).build();
    }

    @Override
    public JsonNode executePayment(String accessToken, String userId,
                                   String walletId, BigDecimal amount, String currency,
                                   String payeeName, String payeeId, String payeeType,
                                   String description, String reference,
                                   String correlationId) {
        Map<String, Object> payee = new LinkedHashMap<>();
        payee.put("name", payeeName);
        payee.put("id", payeeId);
        payee.put("type", payeeType);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("walletId", walletId);
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("payee", payee);
        if (description != null) {
            body.put("description", description);
        }
        body.put("reference", reference);

        return restClient.post()
                .uri("/api/v1/payments")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode getPayment(String accessToken, String userId,
                               String paymentId, String correlationId) {
        return restClient.get()
                .uri("/api/v1/payments/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .retrieve()
                .body(JsonNode.class);
    }

    @Override
    public JsonNode refundPayment(String accessToken, String userId,
                                  String paymentId, BigDecimal amount,
                                  String reason, String reference,
                                  String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (amount != null) {
            body.put("amount", amount);
        }
        if (reason != null) {
            body.put("reason", reason);
        }
        body.put("reference", reference);

        return restClient.post()
                .uri("/api/v1/payments/{paymentId}/refund", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", correlationId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }
}
