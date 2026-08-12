package com.aegis.audit.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a payment audit record.
 *
 * <p>Captures the lifecycle of a payment (requested, executed, failed)
 * for regulatory compliance and auditability.</p>
 *
 * @param id              unique identifier (UUIDv7)
 * @param eventId         identifier of the source domain event
 * @param paymentId       identifier of the payment
 * @param eventType       event type (REQUESTED, EXECUTED, FAILED)
 * @param walletId        payer wallet identifier
 * @param userId          user identifier
 * @param amount          payment amount
 * @param currency        ISO 4217 currency code
 * @param payeeName       payee display name (null when not available)
 * @param failureReason   reason the payment failed (null when not failed)
 * @param correlationId   correlation ID for distributed tracing
 * @param eventTimestamp  timestamp of the original event
 * @param ingestedAt      timestamp when the record was ingested
 */
public record PaymentAuditRecord(
        UUID id,
        UUID eventId,
        UUID paymentId,
        String eventType,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String payeeName,
        String failureReason,
        String correlationId,
        Instant eventTimestamp,
        Instant ingestedAt
) {

    /**
     * Factory method that creates a new PaymentAuditRecord with a generated UUIDv7 identifier.
     *
     * @param eventId         identifier of the source domain event
     * @param paymentId       identifier of the payment
     * @param eventType       event type (REQUESTED, EXECUTED, FAILED)
     * @param walletId        payer wallet identifier
     * @param userId          user identifier
     * @param amount          payment amount
     * @param currency        ISO 4217 currency code
     * @param payeeName       payee display name (null when not available)
     * @param failureReason   reason the payment failed (null when not failed)
     * @param correlationId   correlation ID for distributed tracing
     * @param eventTimestamp  timestamp of the original event
     * @param ingestedAt      timestamp when the record was ingested
     * @return a new PaymentAuditRecord instance
     */
    public static PaymentAuditRecord create(
            UUID eventId,
            UUID paymentId,
            String eventType,
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String payeeName,
            String failureReason,
            String correlationId,
            Instant eventTimestamp,
            Instant ingestedAt
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
        Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");

        return new PaymentAuditRecord(
                UuidV7Generator.generate(),
                eventId,
                paymentId,
                eventType,
                walletId,
                userId,
                amount,
                currency,
                payeeName,
                failureReason,
                correlationId,
                eventTimestamp,
                ingestedAt
        );
    }
}
