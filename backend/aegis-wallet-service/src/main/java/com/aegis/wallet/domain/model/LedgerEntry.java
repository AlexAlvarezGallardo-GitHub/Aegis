package com.aegis.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable entry in a wallet's ledger.
 *
 * <p>Entries are append-only: corrections and reversals are recorded as new
 * entries referencing the original (see {@link #reversalOf()}), never by
 * editing an existing entry (ADR-004).</p>
 *
 * @param id         unique entry identifier (UUIDv7)
 * @param walletId   the wallet the entry belongs to
 * @param type       the entry type
 * @param amount     the absolute, non-negative amount
 * @param currency   ISO-4217 currency code
 * @param reference  external reference (idempotency key for deposits)
 * @param timestamp  when the entry was created
 * @param reversalOf the id of the entry this entry reverses, or {@code null}
 */
public record LedgerEntry(
        UUID id,
        UUID walletId,
        LedgerEntryType type,
        BigDecimal amount,
        String currency,
        String reference,
        Instant timestamp,
        UUID reversalOf
) {
    /**
     * Compatibility constructor for entries that are not reversals.
     */
    public LedgerEntry(UUID id, UUID walletId, LedgerEntryType type, BigDecimal amount,
                       String currency, String reference, Instant timestamp) {
        this(id, walletId, type, amount, currency, reference, timestamp, null);
    }

    public LedgerEntry {
        Objects.requireNonNull(id, "LedgerEntry id must not be null");
        Objects.requireNonNull(walletId, "LedgerEntry walletId must not be null");
        Objects.requireNonNull(type, "LedgerEntry type must not be null");
        Objects.requireNonNull(amount, "LedgerEntry amount must not be null");
        Objects.requireNonNull(currency, "LedgerEntry currency must not be null");
        Objects.requireNonNull(timestamp, "LedgerEntry timestamp must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("LedgerEntry amount must not be negative");
        }
        if (type == LedgerEntryType.REVERSAL && reversalOf == null) {
            throw new IllegalArgumentException("REVERSAL entries must reference the original entry");
        }
    }
}
