package com.aegis.audit.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a payment request.
 * <p>
 * This record is used for Kafka deserialization of the {@code payment.requested} topic.
 * </p>
 *
 * @param eventId       unique identifier of the event
 * @param eventType     type of the event (PAYMENT_REQUESTED)
 * @param schemaVersion version of the event schema
 * @param paymentId     identifier of the payment
 * @param walletId      identifier of the payer wallet
 * @param userId        identifier of the user initiating the payment
 * @param amount        amount to pay
 * @param currency      ISO 4217 currency code
 * @param payee         payment payee
 * @param reference     payment reference
 * @param timestamp     timestamp when the event occurred
 * @param correlationId correlation ID for distributed tracing
 */
public record PaymentRequestedEvent(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Payee payee,
        String reference,
        Instant timestamp,
        String correlationId
) {
}
