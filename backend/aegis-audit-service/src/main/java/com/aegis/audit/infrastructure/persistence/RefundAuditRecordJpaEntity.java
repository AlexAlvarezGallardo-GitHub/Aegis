package com.aegis.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a refund audit record in the database.
 */
@Entity
@Table(name = "refund_audit_records")
public class RefundAuditRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "refund_id", nullable = false)
    private UUID refundId;

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

    @Column
    private String reason;

    @Column(nullable = false)
    private String reference;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected RefundAuditRecordJpaEntity() {
    }

    public RefundAuditRecordJpaEntity(UUID id, UUID eventId, UUID refundId, UUID paymentId,
                                       UUID walletId, UUID userId,
                                       BigDecimal amount, String currency, String reason,
                                       String reference, String correlationId,
                                       Instant eventTimestamp, Instant ingestedAt) {
        this.id = id;
        this.eventId = eventId;
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.reference = reference;
        this.correlationId = correlationId;
        this.eventTimestamp = eventTimestamp;
        this.ingestedAt = ingestedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getRefundId() {
        return refundId;
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

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
