package com.aegis.wallet.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String reference;

    @Column(name = "reversal_of")
    private UUID reversalOf;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntryJpaEntity() {}

    public LedgerEntryJpaEntity(UUID id, UUID walletId, String type, BigDecimal amount,
                                 String currency, String reference, UUID reversalOf, Instant createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.reversalOf = reversalOf;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReference() { return reference; }
    public UUID getReversalOf() { return reversalOf; }
    public Instant getCreatedAt() { return createdAt; }
}
