package com.aegis.audit.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a refund audit record.
 *
 * <p>Captures the details of a payment refund for regulatory compliance
 * and auditability.</p>
 *
 * @param id              unique identifier (UUIDv7)
 * @param eventId         identifier of the source domain event
 * @param refundId        identifier of the refund
 * @param paymentId       identifier of the original payment
 * @param walletId        wallet identifier credited by the refund
 * @param userId          user identifier (refund recipient)
 * @param amount          refund amount
 * @param currency        ISO 4217 currency code
 * @param reason          reason for the refund (null when not provided)
 * @param reference       refund reference
 * @param correlationId   correlation ID for distributed tracing
 * @param eventTimestamp  timestamp of the original event
 * @param ingestedAt      timestamp when the record was ingested
 */
public record RefundAuditRecord(
        UUID id,
        UUID eventId,
        UUID refundId,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reason,
        String reference,
        String correlationId,
        Instant eventTimestamp,
        Instant ingestedAt
) {

    /**
     * Factory method that creates a new RefundAuditRecord with a generated UUIDv7 identifier.
     *
     * @param eventId         identifier of the source domain event
     * @param refundId        identifier of the refund
     * @param paymentId       identifier of the original payment
     * @param walletId        wallet identifier credited by the refund
     * @param userId          user identifier (refund recipient)
     * @param amount          refund amount
     * @param currency        ISO 4217 currency code
     * @param reason          reason for the refund (null when not provided)
     * @param reference       refund reference
     * @param correlationId   correlation ID for distributed tracing
     * @param eventTimestamp  timestamp of the original event
     * @param ingestedAt      timestamp when the record was ingested
     * @return a new RefundAuditRecord instance
     */
    public static RefundAuditRecord create(
            UUID eventId,
            UUID refundId,
            UUID paymentId,
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String reason,
            String reference,
            String correlationId,
            Instant eventTimestamp,
            Instant ingestedAt
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(refundId, "refundId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
        Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");

        return new RefundAuditRecord(
                UuidV7Generator.generate(),
                eventId,
                refundId,
                paymentId,
                walletId,
                userId,
                amount,
                currency,
                reason,
                reference,
                correlationId,
                eventTimestamp,
                ingestedAt
        );
    }
}
