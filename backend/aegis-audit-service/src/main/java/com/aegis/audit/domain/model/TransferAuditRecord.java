package com.aegis.audit.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a transfer audit record.
 *
 * <p>Captures the lifecycle of a funds transfer (requested, completed, failed)
 * for regulatory compliance and auditability.</p>
 *
 * @param id              unique identifier (UUIDv7)
 * @param eventId         identifier of the source domain event
 * @param transferId      identifier of the transfer
 * @param eventType       event type (REQUESTED, COMPLETED, FAILED)
 * @param sourceWalletId  source wallet identifier
 * @param destWalletId    destination wallet identifier
 * @param userId          user identifier
 * @param amount          transfer amount
 * @param currency        ISO 4217 currency code
 * @param reference       transfer reference
 * @param failureReason   reason the transfer failed (null when not failed)
 * @param correlationId   correlation ID for distributed tracing
 * @param eventTimestamp  timestamp of the original event
 * @param ingestedAt      timestamp when the record was ingested
 */
public record TransferAuditRecord(
        UUID id,
        UUID eventId,
        UUID transferId,
        String eventType,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reference,
        String failureReason,
        String correlationId,
        Instant eventTimestamp,
        Instant ingestedAt
) {

    /**
     * Factory method that creates a new TransferAuditRecord with a generated UUIDv7 identifier.
     *
     * @param eventId         identifier of the source domain event
     * @param transferId      identifier of the transfer
     * @param eventType       event type (REQUESTED, COMPLETED, FAILED)
     * @param sourceWalletId  source wallet identifier
     * @param destWalletId    destination wallet identifier
     * @param userId          user identifier
     * @param amount          transfer amount
     * @param currency        ISO 4217 currency code
     * @param reference       transfer reference
     * @param failureReason   reason the transfer failed (null when not failed)
     * @param correlationId   correlation ID for distributed tracing
     * @param eventTimestamp  timestamp of the original event
     * @param ingestedAt      timestamp when the record was ingested
     * @return a new TransferAuditRecord instance
     */
    public static TransferAuditRecord create(
            UUID eventId,
            UUID transferId,
            String eventType,
            UUID sourceWalletId,
            UUID destWalletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String reference,
            String failureReason,
            String correlationId,
            Instant eventTimestamp,
            Instant ingestedAt
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(transferId, "transferId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(sourceWalletId, "sourceWalletId must not be null");
        Objects.requireNonNull(destWalletId, "destWalletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
        Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");

        return new TransferAuditRecord(
                UuidV7Generator.generate(),
                eventId,
                transferId,
                eventType,
                sourceWalletId,
                destWalletId,
                userId,
                amount,
                currency,
                reference,
                failureReason,
                correlationId,
                eventTimestamp,
                ingestedAt
        );
    }
}
