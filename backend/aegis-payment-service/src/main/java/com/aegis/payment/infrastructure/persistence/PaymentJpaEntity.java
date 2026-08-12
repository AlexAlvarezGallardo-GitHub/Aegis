package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code payments} table.
 */
@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payee_name", nullable = false, length = 255)
    private String payeeName;

    @Column(name = "payee_id", nullable = false, length = 255)
    private String payeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payee_type", nullable = false, length = 20)
    private PayeeType payeeType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 255)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "fraud_assessment_id")
    private UUID fraudAssessmentId;

    @Column(name = "hold_id")
    private UUID holdId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PaymentJpaEntity() {
    }

    /**
     * Creates a JPA entity from a domain {@link Payment}.
     *
     * @param payment the domain payment
     */
    public PaymentJpaEntity(Payment payment) {
        this.id = payment.getId();
        this.walletId = payment.getWalletId();
        this.userId = payment.getUserId();
        this.amount = payment.getAmount();
        this.currency = payment.getCurrency();
        this.payeeName = payment.getPayee().name();
        this.payeeId = payment.getPayee().id();
        this.payeeType = payment.getPayee().type();
        this.description = payment.getDescription();
        this.reference = payment.getReference();
        this.status = payment.getStatus();
        this.fraudAssessmentId = payment.getFraudAssessmentId();
        this.holdId = payment.getHoldId();
        this.failureReason = payment.getFailureReason();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
        this.completedAt = payment.getCompletedAt();
    }

    /**
     * Maps this JPA entity back to a domain {@link Payment}.
     *
     * @return the domain payment
     */
    public Payment toDomain() {
        Payee payee = new Payee(payeeName, payeeId, payeeType);
        return Payment.rehydrate(
                id, walletId, userId, amount, currency, payee, description, reference,
                status, fraudAssessmentId, holdId, failureReason,
                createdAt, updatedAt, completedAt
        );
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPayeeName() { return payeeName; }
    public String getPayeeId() { return payeeId; }
    public PayeeType getPayeeType() { return payeeType; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public PaymentStatus getStatus() { return status; }
    public UUID getFraudAssessmentId() { return fraudAssessmentId; }
    public UUID getHoldId() { return holdId; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
