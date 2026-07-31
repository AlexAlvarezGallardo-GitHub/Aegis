package com.aegis.reporting.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a balance projection in the database.
 */
@Entity
@Table(name = "balance_projections")
public class BalanceProjectionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false, unique = true)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected BalanceProjectionJpaEntity() {
    }

    public BalanceProjectionJpaEntity(UUID id, UUID walletId, UUID userId, BigDecimal balance,
                                       String currency, Instant lastUpdated) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
