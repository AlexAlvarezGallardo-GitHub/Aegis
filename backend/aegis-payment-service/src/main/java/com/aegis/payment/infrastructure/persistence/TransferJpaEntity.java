package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;
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
 * JPA entity for the {@code transfers} table.
 */
@Entity
@Table(name = "transfers")
public class TransferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "source_wallet_id", nullable = false)
    private UUID sourceWalletId;

    @Column(name = "dest_wallet_id", nullable = false)
    private UUID destWalletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column
    private String description;

    @Column(nullable = false, length = 255)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

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

    protected TransferJpaEntity() {
    }

    /**
     * Creates a JPA entity from a domain {@link Transfer}.
     *
     * @param transfer the domain transfer
     */
    public TransferJpaEntity(Transfer transfer) {
        this.id = transfer.getId();
        this.sourceWalletId = transfer.getSourceWalletId();
        this.destWalletId = transfer.getDestWalletId();
        this.userId = transfer.getUserId();
        this.amount = transfer.getAmount();
        this.currency = transfer.getCurrency();
        this.description = transfer.getDescription();
        this.reference = transfer.getReference();
        this.status = transfer.getStatus();
        this.fraudAssessmentId = transfer.getFraudAssessmentId();
        this.holdId = transfer.getHoldId();
        this.failureReason = transfer.getFailureReason();
        this.createdAt = transfer.getCreatedAt();
        this.updatedAt = transfer.getUpdatedAt();
        this.completedAt = transfer.getCompletedAt();
    }

    /**
     * Maps this JPA entity back to a domain {@link Transfer}.
     *
     * @return the domain transfer
     */
    public Transfer toDomain() {
        return Transfer.rehydrate(
                id, sourceWalletId, destWalletId, userId,
                amount, currency, description, reference,
                status, fraudAssessmentId, holdId, failureReason,
                createdAt, updatedAt, completedAt
        );
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
