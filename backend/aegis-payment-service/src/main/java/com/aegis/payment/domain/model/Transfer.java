package com.aegis.payment.domain.model;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.exception.InvalidTransferStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transfer aggregate root. Represents a funds-transfer request between two wallets.
 *
 * <p>Rich domain model — state transitions are performed through intent methods that
 * enforce the state machine defined in {@link TransferStatus}. Invalid transitions
 * raise {@link InvalidTransferStateException}.</p>
 */
public class Transfer {

    private final UUID id;
    private final UUID sourceWalletId;
    private final UUID destWalletId;
    private final UUID userId;
    private final BigDecimal amount;
    private final String currency;
    private final String description;
    private final String reference;
    private TransferStatus status;
    private UUID fraudAssessmentId;
    private UUID holdId;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    private Transfer(UUID id, UUID sourceWalletId, UUID destWalletId, UUID userId,
                     BigDecimal amount, String currency, String description, String reference,
                     TransferStatus status, UUID fraudAssessmentId, UUID holdId,
                     String failureReason, Instant createdAt, Instant updatedAt, Instant completedAt) {
        this.id = id;
        this.sourceWalletId = sourceWalletId;
        this.destWalletId = destWalletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.reference = reference;
        this.status = status;
        this.fraudAssessmentId = fraudAssessmentId;
        this.holdId = holdId;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    /**
     * Factory method — creates a new transfer in {@link TransferStatus#PENDING}.
     *
     * @param sourceWalletId source wallet identifier
     * @param destWalletId   destination wallet identifier
     * @param userId         owning user identifier
     * @param amount         transfer amount (positive, scale 2)
     * @param currency       ISO 4217 three-letter currency code
     * @param description    optional human-readable description
     * @param reference      idempotency key
     * @return the newly created transfer
     */
    public static Transfer request(UUID sourceWalletId, UUID destWalletId, UUID userId,
                                   BigDecimal amount, String currency, String description,
                                   String reference) {
        Objects.requireNonNull(sourceWalletId, "sourceWalletId must not be null");
        Objects.requireNonNull(destWalletId, "destWalletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(reference, "reference must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter uppercase ISO code");
        }
        if (sourceWalletId.equals(destWalletId)) {
            throw new IllegalArgumentException("source and destination wallets must differ");
        }

        Instant now = Instant.now();
        return new Transfer(
                UuidV7Generator.generate(),
                sourceWalletId, destWalletId, userId,
                amount.setScale(2, java.math.RoundingMode.HALF_UP),
                currency, description, reference,
                TransferStatus.PENDING,
                null, null, null,
                now, now, null
        );
    }

    /**
     * Rehydrates a transfer from persistence. No validation — the persisted state is trusted.
     */
    public static Transfer rehydrate(UUID id, UUID sourceWalletId, UUID destWalletId, UUID userId,
                                     BigDecimal amount, String currency, String description,
                                     String reference, TransferStatus status,
                                     UUID fraudAssessmentId, UUID holdId, String failureReason,
                                     Instant createdAt, Instant updatedAt, Instant completedAt) {
        return new Transfer(id, sourceWalletId, destWalletId, userId, amount, currency,
                description, reference, status, fraudAssessmentId, holdId, failureReason,
                createdAt, updatedAt, completedAt);
    }

    /**
     * Transitions the transfer to {@link TransferStatus#FRAUD_CHECK}.
     *
     * @throws InvalidTransferStateException if the current state is not {@link TransferStatus#PENDING}
     */
    public void startFraudCheck() {
        ensureTransition(TransferStatus.PENDING, TransferStatus.FRAUD_CHECK);
        this.status = TransferStatus.FRAUD_CHECK;
        this.updatedAt = Instant.now();
    }

    /**
     * Records the fraud assessment identifier once the fraud service has responded.
     *
     * @param assessmentId the fraud assessment identifier
     * @throws InvalidTransferStateException if the current state is not {@link TransferStatus#FRAUD_CHECK}
     */
    public void markFraudAssessed(UUID assessmentId) {
        ensureTransition(TransferStatus.FRAUD_CHECK, TransferStatus.FRAUD_CHECK);
        Objects.requireNonNull(assessmentId, "fraudAssessmentId must not be null");
        this.fraudAssessmentId = assessmentId;
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the transfer to {@link TransferStatus#FUNDS_RESERVED}, recording the hold.
     *
     * @param walletHoldId the wallet hold identifier
     * @throws InvalidTransferStateException if the current state is not {@link TransferStatus#FRAUD_CHECK}
     */
    public void markFundsReserved(UUID walletHoldId) {
        ensureTransition(TransferStatus.FRAUD_CHECK, TransferStatus.FUNDS_RESERVED);
        Objects.requireNonNull(walletHoldId, "holdId must not be null");
        this.holdId = walletHoldId;
        this.status = TransferStatus.FUNDS_RESERVED;
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the transfer to {@link TransferStatus#COMPLETED}.
     *
     * @throws InvalidTransferStateException if the current state is not {@link TransferStatus#FUNDS_RESERVED}
     */
    public void complete() {
        ensureTransition(TransferStatus.FUNDS_RESERVED, TransferStatus.COMPLETED);
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the transfer to {@link TransferStatus#FAILED} with a reason.
     *
     * @param reason human-readable failure reason
     * @throws InvalidTransferStateException if the current state is already terminal
     */
    public void fail(String reason) {
        if (status == TransferStatus.COMPLETED
                || status == TransferStatus.FAILED
                || status == TransferStatus.REVERSED) {
            throw new InvalidTransferStateException(status, TransferStatus.FAILED);
        }
        Objects.requireNonNull(reason, "failure reason must not be null");
        this.failureReason = reason;
        this.status = TransferStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void ensureTransition(TransferStatus expected, TransferStatus intended) {
        if (this.status != expected) {
            throw new InvalidTransferStateException(this.status, intended);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceWalletId() {
        return sourceWalletId;
    }

    public UUID getDestWalletId() {
        return destWalletId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public UUID getFraudAssessmentId() {
        return fraudAssessmentId;
    }

    public UUID getHoldId() {
        return holdId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
