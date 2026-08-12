package com.aegis.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a payment audit record in the database.
 */
@Entity
@Table(name = "payment_audit_records")
public class PaymentAuditRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payee_name")
    private String payeeName;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected PaymentAuditRecordJpaEntity() {
    }

    public PaymentAuditRecordJpaEntity(UUID id, UUID eventId, UUID paymentId, String eventType,
                                        UUID walletId, UUID userId,
                                        BigDecimal amount, String currency, String payeeName,
                                        String failureReason, String correlationId,
                                        Instant eventTimestamp, Instant ingestedAt) {
        this.id = id;
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.payeeName = payeeName;
        this.failureReason = failureReason;
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

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getEventType() {
        return eventType;
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

    public String getPayeeName() {
        return payeeName;
    }

    public String getFailureReason() {
        return failureReason;
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
