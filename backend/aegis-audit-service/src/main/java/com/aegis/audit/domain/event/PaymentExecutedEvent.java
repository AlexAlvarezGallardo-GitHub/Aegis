package com.aegis.audit.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing an executed payment.
 * <p>
 * This record is used for Kafka deserialization of the {@code payment.executed} topic.
 * </p>
 *
 * @param eventId            unique identifier of the event
 * @param eventType          type of the event (PAYMENT_EXECUTED)
 * @param schemaVersion      version of the event schema
 * @param paymentId          identifier of the payment
 * @param walletId           identifier of the payer wallet
 * @param userId             identifier of the user who initiated the payment
 * @param amount             payment amount
 * @param currency           ISO 4217 currency code
 * @param payee              payment payee
 * @param reference          payment reference
 * @param timestamp          timestamp when the event occurred
 * @param correlationId      correlation ID for distributed tracing
 * @param fraudAssessmentId  identifier of the fraud assessment
 * @param holdId             identifier of the settled funds hold
 * @param newBalance         wallet balance after the payment
 */
public record PaymentExecutedEvent(
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
        String correlationId,
        UUID fraudAssessmentId,
        UUID holdId,
        BigDecimal newBalance
) {
}
