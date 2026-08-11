package com.aegis.reporting.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a transfer projection in the database.
 */
@Entity
@Table(name = "transfer_projections")
public class TransferProjectionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false, unique = true)
    private UUID transferId;

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

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    protected TransferProjectionJpaEntity() {
    }

    public TransferProjectionJpaEntity(UUID id, UUID transferId, UUID sourceWalletId, UUID destWalletId,
                                        UUID userId, BigDecimal amount, String currency, String status,
                                        String failureReason, Instant eventTimestamp) {
        this.id = id;
        this.transferId = transferId;
        this.sourceWalletId = sourceWalletId;
        this.destWalletId = destWalletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.failureReason = failureReason;
        this.eventTimestamp = eventTimestamp;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
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

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
}
