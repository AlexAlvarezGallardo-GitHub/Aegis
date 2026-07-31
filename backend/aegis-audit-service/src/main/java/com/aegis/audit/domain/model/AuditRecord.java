package com.aegis.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an audit record.
 * <p>
 * Each record corresponds to a financial event ingested from Kafka,
 * providing a durable audit trail for regulatory compliance.
 * </p>
 */
@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "reference")
    private String reference;

    @Column(name = "new_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * Default constructor required by JPA.
     */
    protected AuditRecord() {
    }

    /**
     * Constructs a new AuditRecord.
     *
     * @param id              Unique identifier
     * @param walletId        Wallet identifier
     * @param userId          User identifier
     * @param amount          Amount deposited
     * @param currency        Currency code
     * @param source          Source of deposit
     * @param reference       Transaction reference
     * @param newBalance      New balance after deposit
     * @param eventTimestamp  Timestamp of the event
     * @param ingestedAt      Timestamp when the record was ingested
     * @param correlationId   Correlation ID for tracing
     */
    public AuditRecord(
            UUID id,
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String source,
            String reference,
            BigDecimal newBalance,
            Instant eventTimestamp,
            Instant ingestedAt,
            String correlationId
    ) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.source = source;
        this.reference = reference;
        this.newBalance = newBalance;
        this.eventTimestamp = eventTimestamp;
        this.ingestedAt = ingestedAt;
        this.correlationId = correlationId;
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

    public String getSource() {
        return source;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
