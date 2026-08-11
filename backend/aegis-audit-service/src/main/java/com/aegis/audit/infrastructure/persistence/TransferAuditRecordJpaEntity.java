package com.aegis.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a transfer audit record in the database.
 */
@Entity
@Table(name = "transfer_audit_records")
public class TransferAuditRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

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
    private String reference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected TransferAuditRecordJpaEntity() {
    }

    public TransferAuditRecordJpaEntity(UUID id, UUID eventId, UUID transferId, String eventType,
                                         UUID sourceWalletId, UUID destWalletId, UUID userId,
                                         BigDecimal amount, String currency, String reference,
                                         String failureReason, String correlationId,
                                         Instant eventTimestamp, Instant ingestedAt) {
        this.id = id;
        this.eventId = eventId;
        this.transferId = transferId;
        this.eventType = eventType;
        this.sourceWalletId = sourceWalletId;
        this.destWalletId = destWalletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
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

    public UUID getTransferId() {
        return transferId;
    }

    public String getEventType() {
        return eventType;
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

    public String getReference() {
        return reference;
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
