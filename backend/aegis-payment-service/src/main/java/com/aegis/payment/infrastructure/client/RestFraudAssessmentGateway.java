package com.aegis.payment.infrastructure.client;

import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway;
import com.aegis.payment.infrastructure.config.FraudProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RestClient-based adapter for the {@link FraudAssessmentGateway} outbound port.
 *
 * <p>Calls the fraud service's {@code POST /api/v1/fraud/assess} endpoint synchronously.
 * Fail-closed: any unavailability (timeout, 5xx, 4xx, network error, unknown decision)
 * results in {@link FraudAssessmentUnavailableException} or {@code REJECT}.</p>
 */
@Component
public class RestFraudAssessmentGateway implements FraudAssessmentGateway {

    private static final Logger log = LoggerFactory.getLogger(RestFraudAssessmentGateway.class);

    private final RestClient restClient;

    public RestFraudAssessmentGateway(RestClient.Builder restClientBuilder,
                                      FraudProperties fraudProperties) {
        this.restClient = restClientBuilder.baseUrl(fraudProperties.baseUrl()).build();
    }

    @Override
    public FraudDecision assess(TransactionContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionId", context.transactionId());
        body.put("transactionType", "TRANSFER");
        body.put("amount", context.amount());
        body.put("currency", context.currency());
        body.put("sourceWalletId", context.sourceWalletId());
        body.put("destWalletId", context.destWalletId());
        body.put("userId", context.userId());

        try {
            JsonNode response = restClient.post()
                    .uri("/api/v1/fraud/assess")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return mapDecision(response);
        } catch (RestClientResponseException ex) {
            log.warn("Fraud service returned error status {} for transaction {}",
                    ex.getStatusCode(), context.transactionId(), ex);
            throw new FraudAssessmentUnavailableException(
                    "Fraud service returned HTTP " + ex.getStatusCode(), ex);
        } catch (ResourceAccessException ex) {
            log.warn("Fraud service unreachable for transaction {}: {}",
                    context.transactionId(), ex.getMessage());
            throw new FraudAssessmentUnavailableException(
                    "Fraud service unreachable: " + ex.getMessage(), ex);
        } catch (FraudAssessmentUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error calling fraud service for transaction {}",
                    context.transactionId(), ex);
            throw new FraudAssessmentUnavailableException(
                    "Unexpected fraud service error: " + ex.getMessage(), ex);
        }
    }

    private FraudDecision mapDecision(JsonNode response) {
        if (response == null || !response.has("decision") || response.get("decision").isNull()) {
            log.warn("Fraud service returned response without decision — failing closed (REJECT)");
            return FraudDecision.REJECT;
        }
        String decision = response.get("decision").asText().trim().toUpperCase();
        return switch (decision) {
            case "APPROVE" -> FraudDecision.APPROVE;
            case "REVIEW" -> FraudDecision.REVIEW;
            case "REJECT" -> FraudDecision.REJECT;
            default -> {
                log.warn("Fraud service returned unknown decision '{}' — failing closed (REJECT)",
                        decision);
                yield FraudDecision.REJECT;
            }
        };
    }
}
