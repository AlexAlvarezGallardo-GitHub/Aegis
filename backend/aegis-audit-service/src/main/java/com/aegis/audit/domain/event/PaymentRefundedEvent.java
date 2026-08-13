package com.aegis.audit.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a refunded payment.
 * <p>
 * This record is used for Kafka deserialization of the {@code payment.refunded} topic.
 * </p>
 *
 * @param eventId       unique identifier of the event
 * @param eventType     type of the event (PAYMENT_REFUNDED)
 * @param schemaVersion version of the event schema
 * @param refundId      identifier of the refund
 * @param paymentId     identifier of the original payment
 * @param walletId      identifier of the wallet credited by the refund
 * @param userId        identifier of the refund recipient
 * @param amount        refund amount
 * @param currency      ISO 4217 currency code
 * @param reason        optional reason for the refund
 * @param reference     refund reference
 * @param newBalance    wallet balance after the refund credit
 * @param timestamp     timestamp when the refund was processed
 * @param correlationId correlation ID for distributed tracing
 */
public record PaymentRefundedEvent(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID refundId,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reason,
        String reference,
        BigDecimal newBalance,
        Instant timestamp,
        String correlationId
) {
}
