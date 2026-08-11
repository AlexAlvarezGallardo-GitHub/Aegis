package com.aegis.wallet.domain.model;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.wallet.domain.exception.HoldNotActiveException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A funds hold (reservation) against a wallet.
 *
 * <p>Reduces the available balance of the source wallet until it is either
 * {@linkplain #settle() settled} (transfer completes), {@linkplain #release() released}
 * (compensation) or {@linkplain #expire() expired} (TTL elapsed).</p>
 */
public class Hold {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int AMOUNT_SCALE = 2;
    private static final String DEFAULT_DESCRIPTION = "Transfer hold";

    private final UUID id;
    private final UUID walletId;
    private final BigDecimal amount;
    private final String currency;
    private final String reference;
    private HoldStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    private Hold(UUID id, UUID walletId, BigDecimal amount, String currency,
                 String reference, HoldStatus status, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Factory that creates a new ACTIVE hold after validating invariants.
     *
     * @param walletId the wallet whose funds are reserved
     * @param amount   the reserved amount (strictly positive, scale 2)
     * @param currency ISO-4217 currency code
     * @param reference the transfer id (idempotency key)
     * @return a new ACTIVE hold
     */
    public static Hold reserve(UUID walletId, BigDecimal amount, String currency, String reference) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(reference, "reference must not be null");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }
        BigDecimal scaled = amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        Instant now = Instant.now();
        return new Hold(
                UuidV7Generator.generate(),
                walletId,
                scaled,
                currency.trim().toUpperCase(),
                reference,
                HoldStatus.ACTIVE,
                now,
                now.plusSeconds(5 * 60)
        );
    }

    /**
     * Reconstructs a hold from persisted state (no validation).
     */
    public static Hold rehydrate(UUID id, UUID walletId, BigDecimal amount, String currency,
                                  String reference, HoldStatus status,
                                  Instant createdAt, Instant expiresAt) {
        return new Hold(id, walletId, amount, currency, reference, status, createdAt, expiresAt);
    }

    /**
     * Transitions this hold from ACTIVE to SETTLED.
     *
     * @throws HoldNotActiveException if the hold is not ACTIVE
     */
    public void settle() {
        if (this.status != HoldStatus.ACTIVE) {
            throw new HoldNotActiveException(
                    "Hold " + id + " is " + status + "; cannot settle");
        }
        this.status = HoldStatus.SETTLED;
    }

    /**
     * Transitions this hold from ACTIVE to RELEASED (compensation).
     *
     * <p>Idempotent: if the hold is already RELEASED this method is a no-op.
     * If the hold is SETTLED or EXPIRED, throws {@link HoldNotActiveException}.</p>
     *
     * @return {@code true} if the hold was ACTIVE and has just been released;
     *         {@code false} if it was already RELEASED (idempotent no-op)
     * @throws HoldNotActiveException if the hold is SETTLED or EXPIRED
     */
    public boolean release() {
        if (this.status == HoldStatus.RELEASED) {
            return false;
        }
        if (this.status != HoldStatus.ACTIVE) {
            throw new HoldNotActiveException(
                    "Hold " + id + " is " + status + "; cannot release");
        }
        this.status = HoldStatus.RELEASED;
        return true;
    }

    /**
     * Transitions this hold from ACTIVE to EXPIRED (TTL elapsed).
     *
     * @throws HoldNotActiveException if the hold is not ACTIVE
     */
    public void expire() {
        if (this.status != HoldStatus.ACTIVE) {
            throw new HoldNotActiveException(
                    "Hold " + id + " is " + status + "; cannot expire");
        }
        this.status = HoldStatus.EXPIRED;
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReference() { return reference; }
    public HoldStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
