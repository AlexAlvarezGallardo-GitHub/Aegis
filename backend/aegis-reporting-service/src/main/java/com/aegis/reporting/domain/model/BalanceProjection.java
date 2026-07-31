package com.aegis.reporting.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a balance projection for a wallet.
 *
 * <p>Read-model that holds the latest known balance for each wallet.
 * Updated whenever a funds deposited event is consumed from Kafka.</p>
 *
 * @param id          unique identifier (UUIDv7)
 * @param walletId    wallet identifier
 * @param userId      user identifier
 * @param balance     current projected balance
 * @param currency    ISO 4217 currency code
 * @param lastUpdated timestamp of the last balance update
 */
public record BalanceProjection(
        UUID id,
        UUID walletId,
        UUID userId,
        BigDecimal balance,
        String currency,
        Instant lastUpdated
) {

    /**
     * Factory method that creates a new BalanceProjection with a generated UUIDv7 identifier.
     *
     * @param walletId    wallet identifier
     * @param userId      user identifier
     * @param balance     current projected balance
     * @param currency    ISO 4217 currency code
     * @param lastUpdated timestamp of the last balance update
     * @return a new BalanceProjection instance
     */
    public static BalanceProjection create(UUID walletId, UUID userId, BigDecimal balance,
                                            String currency, Instant lastUpdated) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");

        return new BalanceProjection(
                UuidV7Generator.generate(),
                walletId,
                userId,
                balance,
                currency,
                lastUpdated
        );
    }

    /**
     * Returns a new BalanceProjection with the updated balance and timestamp.
     *
     * @param newBalance the new wallet balance
     * @param updatedAt  the instant at which the balance changed
     * @return a new BalanceProjection with updated values
     */
    public BalanceProjection withUpdatedBalance(BigDecimal newBalance, Instant updatedAt) {
        return new BalanceProjection(this.id, this.walletId, this.userId, newBalance, this.currency, updatedAt);
    }
}
