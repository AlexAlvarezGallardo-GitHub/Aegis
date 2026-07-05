package com.aegis.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        UUID walletId,
        LedgerEntryType type,
        BigDecimal amount,
        String currency,
        String reference,
        Instant timestamp
) {
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
    }
}
