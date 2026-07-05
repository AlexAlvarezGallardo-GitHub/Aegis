package com.aegis.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    void createWalletShouldSetInitialBalanceToZero() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");

        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertEquals("EUR", wallet.getCurrency());
    }

    @Test
    void createWalletShouldAddOpeningLedgerEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");

        assertEquals(1, wallet.getLedgerEntries().size());
        LedgerEntry entry = wallet.getLedgerEntries().getFirst();
        assertEquals(LedgerEntryType.OPENING, entry.type());
        assertEquals(BigDecimal.ZERO, entry.amount());
    }

    @Test
    void createWalletShouldThrowOnInvalidCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> Wallet.create(UUID.randomUUID(), "INVALID"));
    }

    @Test
    void createWalletShouldThrowOnBlankCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> Wallet.create(UUID.randomUUID(), ""));
    }

    @Test
    void createWalletShouldThrowOnNullCurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> Wallet.create(UUID.randomUUID(), null));
    }

    @Test
    void createWalletShouldNormalizeCurrency() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "eur");
        assertEquals("EUR", wallet.getCurrency());
    }

    @Test
    void walletShouldGenerateValidId() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "USD");
        assertNotNull(wallet.getWalletId().value());
    }

    @Test
    void walletShouldReturnCopyOfLedgerEntries() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(UnsupportedOperationException.class,
                () -> wallet.getLedgerEntries().clear());
    }

    @Test
    void rehydrateShouldPreserveState() {
        WalletId walletId = WalletId.generate();
        UUID userId = UUID.randomUUID();
        Instant now = java.time.Instant.now();
        LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(), walletId.value(),
                LedgerEntryType.OPENING, BigDecimal.ZERO,
                "EUR", "test", now
        );

        Wallet wallet = Wallet.rehydrate(
                walletId, userId, BigDecimal.ZERO, "EUR",
                WalletStatus.ACTIVE, now, now, 0L,
                java.util.List.of(entry)
        );

        assertEquals(walletId, wallet.getWalletId());
        assertEquals(userId, wallet.getUserId());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertEquals(1, wallet.getLedgerEntries().size());
    }

    @Test
    void toCreatedEventShouldIncludeWalletDetails() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.create(userId, "EUR");
        String correlationId = "corr-123";

        var event = wallet.toCreatedEvent(correlationId);

        assertEquals(wallet.getWalletId().value(), event.walletId());
        assertEquals(userId, event.userId());
        assertEquals("EUR", event.currency());
        assertEquals(correlationId, event.correlationId());
        assertEquals("WALLET_CREATED", event.eventType());
    }
}
