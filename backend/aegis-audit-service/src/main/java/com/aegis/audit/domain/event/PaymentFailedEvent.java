package com.aegis.audit.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a failed payment.
 * <p>
 * This record is used for Kafka deserialization of the {@code payment.failed} topic.
 * </p>
 *
 * @param eventId         unique identifier of the event
 * @param eventType       type of the event (PAYMENT_FAILED)
 * @param schemaVersion   version of the event schema
 * @param paymentId       identifier of the payment
 * @param walletId        identifier of the payer wallet
 * @param userId          identifier of the user who initiated the payment
 * @param amount          payment amount
 * @param currency        ISO 4217 currency code
 * @param reference       payment reference
 * @param timestamp       timestamp when the event occurred
 * @param correlationId   correlation ID for distributed tracing
 * @param failureReason   reason the payment failed
 * @param failureDetails  optional details about the failure
 * @param compensated     whether compensating actions were executed
 */
public record PaymentFailedEvent(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reference,
        Instant timestamp,
        String correlationId,
        String failureReason,
        String failureDetails,
        boolean compensated
) {
}
