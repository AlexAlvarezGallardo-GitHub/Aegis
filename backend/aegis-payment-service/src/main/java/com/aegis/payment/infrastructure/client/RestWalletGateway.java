package com.aegis.payment.infrastructure.client;

import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.port.outbound.WalletGateway;
import com.aegis.payment.infrastructure.config.WalletProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RestClient-based adapter for the {@link WalletGateway} outbound port.
 *
 * <p>Drives the wallet service's internal hold/settle/release endpoints used by the
 * transfer saga (specs/005-transfer-funds/contracts/api/wallet-transfer-api.yaml).
 * Any unavailability or unexpected status is mapped to {@link SettlementFailedException},
 * which triggers saga compensation (hold release).</p>
 */
@Component
public class RestWalletGateway implements WalletGateway {

    private static final Logger log = LoggerFactory.getLogger(RestWalletGateway.class);

    private final RestClient restClient;

    public RestWalletGateway(RestClient.Builder restClientBuilder, WalletProperties walletProperties) {
        this.restClient = restClientBuilder.baseUrl(walletProperties.baseUrl()).build();
    }

    @Override
    public UUID createHold(UUID sourceWalletId, BigDecimal amount, String currency, String reference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("reference", reference);

        try {
            JsonNode response = restClient.post()
                    .uri("/api/v1/wallets/{walletId}/holds", sourceWalletId)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("holdId") || response.get("holdId").isNull()) {
                throw new SettlementFailedException("Wallet service did not return a hold id");
            }
            return UUID.fromString(response.get("holdId").asText());
        } catch (RestClientResponseException ex) {
            log.warn("Wallet service rejected hold for wallet {}: HTTP {}", sourceWalletId, ex.getStatusCode());
            String code = extractErrorCode(ex);
            throw new SettlementFailedException(
                    code, "Wallet service rejected hold (HTTP " + ex.getStatusCode() + ")", ex);
        } catch (ResourceAccessException ex) {
            log.warn("Wallet service unreachable while creating hold for wallet {}: {}",
                    sourceWalletId, ex.getMessage());
            throw new SettlementFailedException(
                    "Wallet service unreachable: " + ex.getMessage(), ex);
        } catch (SettlementFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating hold for wallet {}", sourceWalletId, ex);
            throw new SettlementFailedException(
                    "Unexpected wallet service error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public SettlementResult settle(UUID transferId, UUID holdId, UUID sourceWalletId, UUID destWalletId,
                                   BigDecimal amount, String currency) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transferId", transferId);
        body.put("holdId", holdId);
        body.put("sourceWalletId", sourceWalletId);
        body.put("destWalletId", destWalletId);
        body.put("amount", amount);
        body.put("currency", currency);

        try {
            JsonNode response = restClient.post()
                    .uri("/api/v1/wallets/transfers/settle")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("sourceNewBalance")) {
                throw new SettlementFailedException("Wallet service did not return settlement balances");
            }
            return new SettlementResult(
                    response.get("sourceNewBalance").decimalValue(),
                    response.get("destNewBalance").decimalValue()
            );
        } catch (RestClientResponseException ex) {
            log.warn("Wallet service rejected settlement for transfer {}: HTTP {}",
                    transferId, ex.getStatusCode());
            throw new SettlementFailedException(
                    "Wallet service rejected settlement (HTTP " + ex.getStatusCode() + ")", ex);
        } catch (ResourceAccessException ex) {
            log.warn("Wallet service unreachable during settlement of transfer {}: {}",
                    transferId, ex.getMessage());
            throw new SettlementFailedException(
                    "Wallet service unreachable: " + ex.getMessage(), ex);
        } catch (SettlementFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error settling transfer {}", transferId, ex);
            throw new SettlementFailedException(
                    "Unexpected wallet service error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void release(UUID walletId, UUID holdId) {
        try {
            restClient.post()
                    .uri("/api/v1/wallets/{walletId}/holds/{holdId}/release", walletId, holdId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Wallet service could not release hold {} (wallet {}): HTTP {}",
                    holdId, walletId, ex.getStatusCode());
            throw new SettlementFailedException(
                    "Wallet service could not release hold (HTTP " + ex.getStatusCode() + ")", ex);
        } catch (ResourceAccessException ex) {
            log.warn("Wallet service unreachable while releasing hold {}: {}", holdId, ex.getMessage());
            throw new SettlementFailedException(
                    "Wallet service unreachable: " + ex.getMessage(), ex);
        } catch (SettlementFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error releasing hold {}", holdId, ex);
            throw new SettlementFailedException(
                    "Unexpected wallet service error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public BigDecimal debitHold(UUID paymentId, UUID holdId, UUID walletId,
                                BigDecimal amount, String currency) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentId", paymentId);
        body.put("holdId", holdId);
        body.put("amount", amount);
        body.put("currency", currency);

        try {
            JsonNode response = restClient.post()
                    .uri("/api/v1/wallets/{walletId}/holds/{holdId}/debit", walletId, holdId)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("newBalance")) {
                throw new SettlementFailedException("Wallet service did not return new balance after debit");
            }
            return response.get("newBalance").decimalValue();
        } catch (RestClientResponseException ex) {
            log.warn("Wallet service rejected debit for payment {}: HTTP {}",
                    paymentId, ex.getStatusCode());
            String code = extractErrorCode(ex);
            throw new SettlementFailedException(
                    code, "Wallet service rejected debit (HTTP " + ex.getStatusCode() + ")", ex);
        } catch (ResourceAccessException ex) {
            log.warn("Wallet service unreachable during debit of payment {}: {}",
                    paymentId, ex.getMessage());
            throw new SettlementFailedException(
                    "Wallet service unreachable: " + ex.getMessage(), ex);
        } catch (SettlementFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error debiting payment {}", paymentId, ex);
            throw new SettlementFailedException(
                    "Unexpected wallet service error: " + ex.getMessage(), ex);
        }
    }

    private String extractErrorCode(RestClientResponseException ex) {
        try {
            JsonNode body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(ex.getResponseBodyAsString());
            if (body != null && body.hasNonNull("code")) {
                return body.get("code").asText();
            }
        } catch (Exception ignore) {
            // fall through to the generic code
        }
        return "SETTLEMENT_FAILED";
    }
}
