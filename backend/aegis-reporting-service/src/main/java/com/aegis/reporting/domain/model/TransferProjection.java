package com.aegis.reporting.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a transfer projection.
 *
 * <p>Read-model that holds the latest known status for each transfer.
 * Updated whenever a transfer completed or failed event is consumed from Kafka.</p>
 *
 * @param id              unique identifier (UUIDv7)
 * @param transferId      transfer identifier
 * @param sourceWalletId  source wallet identifier
 * @param destWalletId    destination wallet identifier
 * @param userId          user identifier
 * @param amount          transfer amount
 * @param currency        ISO 4217 currency code
 * @param status          transfer status (COMPLETED, FAILED)
 * @param failureReason   reason the transfer failed (null when not failed)
 * @param eventTimestamp  timestamp of the last event
 */
public record TransferProjection(
        UUID id,
        UUID transferId,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        Instant eventTimestamp
) {

    /**
     * Factory method that creates a new TransferProjection with a generated UUIDv7 identifier.
     *
     * @param transferId      transfer identifier
     * @param sourceWalletId  source wallet identifier
     * @param destWalletId    destination wallet identifier
     * @param userId          user identifier
     * @param amount          transfer amount
     * @param currency        ISO 4217 currency code
     * @param status          transfer status (COMPLETED, FAILED)
     * @param failureReason   reason the transfer failed (null when not failed)
     * @param eventTimestamp  timestamp of the event
     * @return a new TransferProjection instance
     */
    public static TransferProjection create(UUID transferId, UUID sourceWalletId, UUID destWalletId,
                                             UUID userId, BigDecimal amount, String currency,
                                             String status, String failureReason, Instant eventTimestamp) {
        Objects.requireNonNull(transferId, "transferId must not be null");
        Objects.requireNonNull(sourceWalletId, "sourceWalletId must not be null");
        Objects.requireNonNull(destWalletId, "destWalletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");

        return new TransferProjection(
                UuidV7Generator.generate(),
                transferId,
                sourceWalletId,
                destWalletId,
                userId,
                amount,
                currency,
                status,
                failureReason,
                eventTimestamp
        );
    }

    /**
     * Returns a new TransferProjection with the updated status, failure reason and timestamp.
     *
     * @param newStatus      the new transfer status
     * @param newFailureReason the failure reason (null when not failed)
     * @param updatedAt      the instant at which the status changed
     * @return a new TransferProjection with updated values
     */
    public TransferProjection withStatus(String newStatus, String newFailureReason, Instant updatedAt) {
        return new TransferProjection(
                this.id, this.transferId, this.sourceWalletId, this.destWalletId,
                this.userId, this.amount, this.currency, newStatus, newFailureReason, updatedAt
        );
    }
}
