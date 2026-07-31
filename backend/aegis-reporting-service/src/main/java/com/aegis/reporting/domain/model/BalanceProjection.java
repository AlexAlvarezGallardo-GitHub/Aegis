package com.aegis.reporting.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-model entity that holds the latest known balance for each wallet.
 * Updated whenever a {@code FundsDepositedEvent} is consumed from Kafka.
 */
@Entity
@Table(name = "balance_projections")
public class BalanceProjection {

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

    protected BalanceProjection() {}

    public BalanceProjection(UUID id, UUID walletId, UUID userId, BigDecimal balance,
                             String currency, Instant lastUpdated) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public Instant getLastUpdated() { return lastUpdated; }

    /**
     * Updates the projected balance and the last-updated timestamp.
     *
     * @param newBalance  the new wallet balance
     * @param lastUpdated the instant at which the balance changed
     */
    public void updateBalance(BigDecimal newBalance, Instant lastUpdated) {
        this.balance = newBalance;
        this.lastUpdated = lastUpdated;
    }
}
