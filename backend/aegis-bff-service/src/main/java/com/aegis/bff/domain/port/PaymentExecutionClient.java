package com.aegis.bff.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * Outbound port for executing payments against the Payment Service.
 *
 * <p>Unlike {@link PaymentClient} (which handles wallet-to-wallet transfers),
 * this client handles payments to external payees (merchants, individuals, services).</p>
 */
public interface PaymentExecutionClient {

    /**
     * Executes a payment from a wallet to a payee.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id extracted from the session JWT
     * @param walletId      the source wallet id
     * @param amount        the amount to pay
     * @param currency      the 3-letter ISO 4217 currency code
     * @param payeeName     the payee name
     * @param payeeId       the payee identifier
     * @param payeeType     the payee type (MERCHANT, INDIVIDUAL, SERVICE)
     * @param description   an optional description
     * @param reference     the idempotency / external reference
     * @param correlationId the correlation id for tracing
     * @return the created payment
     */
    JsonNode executePayment(String accessToken, String userId,
                            String walletId, BigDecimal amount, String currency,
                            String payeeName, String payeeId, String payeeType,
                            String description, String reference,
                            String correlationId);

    /**
     * Retrieves a payment by id.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id extracted from the session JWT
     * @param paymentId     the payment id
     * @param correlationId the correlation id for tracing
     * @return the payment
     */
    JsonNode getPayment(String accessToken, String userId,
                        String paymentId, String correlationId);

    /**
     * Refunds a completed payment.
     *
     * @param accessToken   the bearer token
     * @param userId        the user id extracted from the session JWT
     * @param paymentId     the payment id to refund
     * @param amount        the refund amount (null for full refund)
     * @param reason        the optional refund reason
     * @param reference     the idempotency / external reference
     * @param correlationId the correlation id for tracing
     * @return the created refund
     */
    JsonNode refundPayment(String accessToken, String userId,
                           String paymentId, BigDecimal amount,
                           String reason, String reference,
                           String correlationId);
}
