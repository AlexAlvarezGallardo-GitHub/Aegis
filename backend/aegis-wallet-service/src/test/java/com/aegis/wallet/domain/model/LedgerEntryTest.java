package com.aegis.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LedgerEntryTest {

    @Test
    void createLedgerEntryShouldSucceed() {
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();

        LedgerEntry entry = new LedgerEntry(
                id, walletId, LedgerEntryType.DEPOSIT,
                new BigDecimal("100.00"), "EUR", "ref-1", Instant.now()
        );

        assertEquals(id, entry.id());
        assertEquals(walletId, entry.walletId());
        assertEquals(LedgerEntryType.DEPOSIT, entry.type());
        assertEquals(new BigDecimal("100.00"), entry.amount());
    }

    @Test
    void createLedgerEntryShouldThrowOnNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEntry(
                        UUID.randomUUID(), UUID.randomUUID(), LedgerEntryType.DEPOSIT,
                        new BigDecimal("-10.00"), "EUR", null, Instant.now()
                ));
    }

    @Test
    void createLedgerEntryShouldThrowOnNullId() {
        assertThrows(NullPointerException.class,
                () -> new LedgerEntry(
                        null, UUID.randomUUID(), LedgerEntryType.DEPOSIT,
                        BigDecimal.TEN, "EUR", null, Instant.now()
                ));
    }
}
