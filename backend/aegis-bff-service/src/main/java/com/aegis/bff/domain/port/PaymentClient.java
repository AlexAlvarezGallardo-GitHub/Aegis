package com.aegis.bff.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * Outbound port for communicating with the Payment Service.
 */
public interface PaymentClient {

    /**
     * Initiates a funds transfer between two wallets.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id extracted from the session JWT
     * @param sourceWalletId the source wallet id
     * @param destWalletId   the destination wallet id
     * @param amount         the amount to transfer
     * @param currency       the 3-letter ISO 4217 currency code
     * @param description    an optional description for the transfer
     * @param reference      the idempotency / external reference
     * @param correlationId  the correlation id for tracing
     * @return the created transfer
     */
    JsonNode transferFunds(String accessToken, String userId,
                           String sourceWalletId, String destWalletId,
                           BigDecimal amount, String currency,
                           String description, String reference,
                           String correlationId);

    /**
     * Retrieves a transfer by id.
     *
     * @param accessToken  the bearer token
     * @param userId       the user id extracted from the session JWT
     * @param transferId   the transfer id
     * @param correlationId the correlation id for tracing
     * @return the transfer
     */
    JsonNode getTransfer(String accessToken, String userId,
                         String transferId, String correlationId);
}
