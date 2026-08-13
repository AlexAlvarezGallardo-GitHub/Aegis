package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Refund;
import com.aegis.payment.domain.model.RefundStatus;
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
 * JPA entity for the {@code refunds} table.
 */
@Entity
@Table(name = "refunds")
public class RefundJpaEntity {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false, length = 255)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected RefundJpaEntity() {
    }

    /**
     * Creates a JPA entity from a domain {@link Refund}.
     *
     * @param refund the domain refund
     */
    public RefundJpaEntity(Refund refund) {
        this.id = refund.getId();
        this.paymentId = refund.getPaymentId();
        this.walletId = refund.getWalletId();
        this.userId = refund.getUserId();
        this.amount = refund.getAmount();
        this.currency = refund.getCurrency();
        this.reason = refund.getReason();
        this.reference = refund.getReference();
        this.status = refund.getStatus();
        this.createdAt = refund.getCreatedAt();
        this.updatedAt = refund.getUpdatedAt();
        this.completedAt = refund.getCompletedAt();
    }

    /**
     * Maps this JPA entity back to a domain {@link Refund}.
     *
     * @return the domain refund
     */
    public Refund toDomain() {
        return Refund.rehydrate(
                id, paymentId, walletId, userId, amount, currency, reason, reference,
                status, createdAt, updatedAt, completedAt
        );
    }

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getWalletId() { return walletId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReason() { return reason; }
    public String getReference() { return reference; }
    public RefundStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
