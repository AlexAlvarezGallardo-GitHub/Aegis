package com.aegis.wallet.domain.model;

import com.aegis.wallet.domain.exception.InsufficientFundsException;
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
    void depositFundsShouldIncreaseBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-123", "Test deposit");

        assertEquals(0, new BigDecimal("100.00").compareTo(wallet.getBalance()));
        assertEquals(2, wallet.getLedgerEntries().size());
        LedgerEntry entry = wallet.getLedgerEntries().get(1);
        assertEquals(LedgerEntryType.DEPOSIT, entry.type());
        assertEquals("TXN-123", entry.reference());
    }

    @Test
    void depositFundsShouldRejectNegativeAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(IllegalArgumentException.class,
                () -> wallet.depositFunds(new BigDecimal("-50.00"), "BANK_TRANSFER", "TXN-123", null));
    }

    @Test
    void depositFundsShouldRejectZeroAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(IllegalArgumentException.class,
                () -> wallet.depositFunds(BigDecimal.ZERO, "BANK_TRANSFER", "TXN-123", null));
    }

    @Test
    void depositFundsShouldRejectWhenWalletNotActive() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.deactivate(WalletStatus.FROZEN);
        assertThrows(com.aegis.wallet.domain.exception.WalletOperationNotAllowedException.class,
                () -> wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-123", null));
    }

    @Test
    void depositFundsShouldUseReferenceWhenDescriptionMissing() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("50.00"), "CARD", "REF-001", null);

        LedgerEntry entry = wallet.getLedgerEntries().get(1);
        assertEquals("REF-001", entry.reference());
    }

    @Test
    void toFundsDepositedEventShouldIncludeFundsDepositedDetails() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.create(userId, "EUR");
        wallet.depositFunds(new BigDecimal("200.00"), "BANK_TRANSFER", "TXN-456", null);

        var event = wallet.toFundsDepositedEvent(new BigDecimal("200.00"), "BANK_TRANSFER", "TXN-456", "corr-1");

        assertEquals(wallet.getWalletId().value(), event.walletId());
        assertEquals(userId, event.userId());
        assertEquals("BANK_TRANSFER", event.source());
        assertEquals("TXN-456", event.reference());
        assertEquals("FUNDS_DEPOSITED", event.eventType());
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

    @Test
    void adjustBalanceShouldRejectWithdrawalThatWouldMakeBalanceNegative() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("50.00"), "BANK_TRANSFER", "TXN-001", null);

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> wallet.adjustBalance(new BigDecimal("-100.00"), "Overdraw"));
        assertEquals("INSUFFICIENT_FUNDS", ex.getCode());
    }

    @Test
    void adjustBalanceShouldAllowWithdrawalWithinBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-001", null);

        wallet.adjustBalance(new BigDecimal("-50.00"), "Withdrawal");

        assertEquals(0, new BigDecimal("50.00").compareTo(wallet.getBalance()));
    }

    @Test
    void adjustBalanceShouldAllowExactBalanceWithdrawal() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-001", null);

        wallet.adjustBalance(new BigDecimal("-100.00"), "Full withdrawal");

        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
    }
}
