package com.aegis.payment.domain.model;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.exception.InvalidPaymentStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment aggregate root. Represents a payment request to a payee from a single wallet.
 *
 * <p>Rich domain model — state transitions are performed through intent methods that
 * enforce the state machine defined in {@link PaymentStatus}. Invalid transitions
 * raise {@link InvalidPaymentStateException}.</p>
 */
public class Payment {

    private final UUID id;
    private final UUID walletId;
    private final UUID userId;
    private final BigDecimal amount;
    private final String currency;
    private final Payee payee;
    private final String description;
    private final String reference;
    private PaymentStatus status;
    private UUID fraudAssessmentId;
    private UUID holdId;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    private Payment(UUID id, UUID walletId, UUID userId, BigDecimal amount, String currency,
                    Payee payee, String description, String reference, PaymentStatus status,
                    UUID fraudAssessmentId, UUID holdId, String failureReason,
                    Instant createdAt, Instant updatedAt, Instant completedAt) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.payee = payee;
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
     * Factory method — creates a new payment in {@link PaymentStatus#PENDING}.
     *
     * @param walletId    the wallet to debit
     * @param userId      owning user identifier
     * @param amount      payment amount (positive, scale 2)
     * @param currency    ISO 4217 three-letter currency code
     * @param payee       the payment recipient
     * @param description optional human-readable description
     * @param reference   idempotency key
     * @return the newly created payment
     */
    public static Payment request(UUID walletId, UUID userId, BigDecimal amount, String currency,
                                  Payee payee, String description, String reference) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(payee, "payee must not be null");
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
        return new Payment(
                UuidV7Generator.generate(),
                walletId, userId,
                amount.setScale(2, RoundingMode.HALF_UP),
                currency, payee, description, reference,
                PaymentStatus.PENDING,
                null, null, null,
                now, now, null
        );
    }

    /**
     * Rehydrates a payment from persistence. No validation — the persisted state is trusted.
     */
    public static Payment rehydrate(UUID id, UUID walletId, UUID userId,
                                    BigDecimal amount, String currency,
                                    Payee payee, String description, String reference,
                                    PaymentStatus status,
                                    UUID fraudAssessmentId, UUID holdId, String failureReason,
                                    Instant createdAt, Instant updatedAt, Instant completedAt) {
        return new Payment(id, walletId, userId, amount, currency, payee, description, reference,
                status, fraudAssessmentId, holdId, failureReason, createdAt, updatedAt, completedAt);
    }

    /**
     * Transitions the payment to {@link PaymentStatus#PROCESSING}.
     *
     * @throws InvalidPaymentStateException if the current state is not {@link PaymentStatus#PENDING}
     */
    public void startProcessing() {
        ensureTransition(PaymentStatus.PENDING, PaymentStatus.PROCESSING);
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    /**
     * Records the fraud assessment identifier once the fraud service has responded.
     *
     * @param assessmentId the fraud assessment identifier
     */
    public void markFraudAssessed(UUID assessmentId) {
        Objects.requireNonNull(assessmentId, "fraudAssessmentId must not be null");
        this.fraudAssessmentId = assessmentId;
        this.updatedAt = Instant.now();
    }

    /**
     * Records the hold identifier once funds have been reserved.
     *
     * @param walletHoldId the wallet hold identifier
     * @throws InvalidPaymentStateException if the current state is not {@link PaymentStatus#PROCESSING}
     */
    public void markFundsReserved(UUID walletHoldId) {
        ensureTransition(PaymentStatus.PROCESSING, PaymentStatus.PROCESSING);
        Objects.requireNonNull(walletHoldId, "holdId must not be null");
        this.holdId = walletHoldId;
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the payment to {@link PaymentStatus#COMPLETED}.
     *
     * @throws InvalidPaymentStateException if the current state is not {@link PaymentStatus#PROCESSING}
     */
    public void complete() {
        ensureTransition(PaymentStatus.PROCESSING, PaymentStatus.COMPLETED);
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the payment to {@link PaymentStatus#FAILED} with a reason.
     *
     * @param reason machine-readable failure reason
     * @throws InvalidPaymentStateException if the current state is already terminal
     */
    public void fail(String reason) {
        if (status == PaymentStatus.COMPLETED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.REFUNDED) {
            throw new InvalidPaymentStateException(status, PaymentStatus.FAILED);
        }
        Objects.requireNonNull(reason, "failure reason must not be null");
        this.failureReason = reason;
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    private void ensureTransition(PaymentStatus expected, PaymentStatus intended) {
        if (this.status != expected) {
            throw new InvalidPaymentStateException(this.status, intended);
        }
    }

    public UUID getId() {
        return id;
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

    public Payee getPayee() {
        return payee;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public PaymentStatus getStatus() {
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
