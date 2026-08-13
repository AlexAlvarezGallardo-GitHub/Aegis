package com.aegis.payment.domain.model;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.exception.InvalidRefundStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Refund aggregate root. Represents a refund of a completed payment, crediting
 * the payer's wallet with a REFUND ledger entry.
 *
 * <p>Rich domain model — state transitions are performed through intent methods that
 * enforce the state machine defined in {@link RefundStatus}. Invalid transitions
 * raise {@link InvalidRefundStateException}.</p>
 */
public class Refund {

    private final UUID id;
    private final UUID paymentId;
    private final UUID walletId;
    private final UUID userId;
    private final BigDecimal amount;
    private final String currency;
    private final String reason;
    private final String reference;
    private RefundStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    private Refund(UUID id, UUID paymentId, UUID walletId, UUID userId, BigDecimal amount,
                   String currency, String reason, String reference, RefundStatus status,
                   Instant createdAt, Instant updatedAt, Instant completedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.reference = reference;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    /**
     * Factory method — creates a new refund in {@link RefundStatus#PENDING}.
     *
     * @param paymentId the payment to refund
     * @param walletId  the wallet to credit
     * @param userId    the refund recipient
     * @param amount    the refund amount (positive, scale 2)
     * @param currency  ISO 4217 three-letter currency code
     * @param reason    optional refund reason
     * @param reference idempotency key
     * @return the newly created refund
     */
    public static Refund request(UUID paymentId, UUID walletId, UUID userId, BigDecimal amount,
                                 String currency, String reason, String reference) {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
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
        if (reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }

        Instant now = Instant.now();
        return new Refund(
                UuidV7Generator.generate(),
                paymentId, walletId, userId,
                amount.setScale(2, RoundingMode.HALF_UP),
                currency, reason, reference,
                RefundStatus.PENDING,
                now, now, null
        );
    }

    /**
     * Rehydrates a refund from persistence. No validation — the persisted state is trusted.
     */
    public static Refund rehydrate(UUID id, UUID paymentId, UUID walletId, UUID userId,
                                   BigDecimal amount, String currency, String reason, String reference,
                                   RefundStatus status, Instant createdAt, Instant updatedAt,
                                   Instant completedAt) {
        return new Refund(id, paymentId, walletId, userId, amount, currency, reason, reference,
                status, createdAt, updatedAt, completedAt);
    }

    /**
     * Transitions the refund to {@link RefundStatus#COMPLETED}.
     *
     * @throws InvalidRefundStateException if the current state is not {@link RefundStatus#PENDING}
     */
    public void complete() {
        ensureTransition(RefundStatus.PENDING, RefundStatus.COMPLETED);
        this.status = RefundStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the refund to {@link RefundStatus#FAILED} with a reason.
     *
     * @param failureReason machine-readable failure reason
     * @throws InvalidRefundStateException if the current state is not {@link RefundStatus#PENDING}
     */
    public void fail(String failureReason) {
        ensureTransition(RefundStatus.PENDING, RefundStatus.FAILED);
        Objects.requireNonNull(failureReason, "failure reason must not be null");
        this.status = RefundStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void ensureTransition(RefundStatus expected, RefundStatus intended) {
        if (this.status != expected) {
            throw new InvalidRefundStateException(this.status, intended);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getWalletId() {
        return walletId;
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

    public String getReason() {
        return reason;
    }

    public String getReference() {
        return reference;
    }

    public RefundStatus getStatus() {
        return status;
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
