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

    @Test
    void reverseDepositShouldReduceBalanceAndAppendReversalEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-001", null);
        UUID depositId = wallet.getLedgerEntries().get(1).id();

        LedgerEntry reversal = wallet.reverseDeposit(depositId, "REV-001", null);

        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
        assertEquals(LedgerEntryType.REVERSAL, reversal.type());
        assertEquals(depositId, reversal.reversalOf());
        assertEquals(0, new BigDecimal("100.00").compareTo(reversal.amount()));
        // both entries remain in the ledger (immutable history)
        assertEquals(3, wallet.getLedgerEntries().size());
    }

    @Test
    void reverseDepositShouldRejectUnknownEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(com.aegis.wallet.domain.exception.DepositReversalException.class,
                () -> wallet.reverseDeposit(UUID.randomUUID(), "REV-001", null));
    }

    @Test
    void reverseDepositShouldRejectNonDepositEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        UUID openingId = wallet.getLedgerEntries().getFirst().id();
        assertThrows(com.aegis.wallet.domain.exception.DepositReversalException.class,
                () -> wallet.reverseDeposit(openingId, "REV-001", null));
    }

    @Test
    void reverseDepositShouldRejectSecondReversal() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-001", null);
        UUID depositId = wallet.getLedgerEntries().get(1).id();

        wallet.reverseDeposit(depositId, "REV-001", null);
        assertThrows(com.aegis.wallet.domain.exception.DepositReversalException.class,
                () -> wallet.reverseDeposit(depositId, "REV-002", null));
    }

    @Test
    void debitForTransferShouldDecreaseBalanceAndAppendTransferOutEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("200.00"), "BANK_TRANSFER", "TXN-001", null);

        wallet.debitForTransfer(new BigDecimal("75.00"), "TRANSFER-1", "Transfer out");

        assertEquals(0, new BigDecimal("125.00").compareTo(wallet.getBalance()));
        LedgerEntry last = wallet.getLedgerEntries().getLast();
        assertEquals(LedgerEntryType.TRANSFER_OUT, last.type());
        assertEquals(0, new BigDecimal("75.00").compareTo(last.amount()));
        assertEquals("TRANSFER-1", last.reference());
    }

    @Test
    void debitForTransferShouldThrowWhenInsufficientBalance() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("50.00"), "BANK_TRANSFER", "TXN-001", null);

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> wallet.debitForTransfer(new BigDecimal("100.00"), "TRANSFER-1", null));
        assertEquals("INSUFFICIENT_FUNDS", ex.getCode());
    }

    @Test
    void debitForTransferShouldThrowWhenWalletNotActive() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.deactivate(WalletStatus.FROZEN);

        assertThrows(com.aegis.wallet.domain.exception.WalletOperationNotAllowedException.class,
                () -> wallet.debitForTransfer(new BigDecimal("10.00"), "TRANSFER-1", null));
    }

    @Test
    void debitForTransferShouldRejectNonPositiveAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(IllegalArgumentException.class,
                () -> wallet.debitForTransfer(BigDecimal.ZERO, "TRANSFER-1", null));
        assertThrows(IllegalArgumentException.class,
                () -> wallet.debitForTransfer(new BigDecimal("-5.00"), "TRANSFER-1", null));
    }

    @Test
    void creditForTransferShouldIncreaseBalanceAndAppendTransferInEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");

        wallet.creditForTransfer(new BigDecimal("150.00"), "TRANSFER-1", "Transfer in");

        assertEquals(0, new BigDecimal("150.00").compareTo(wallet.getBalance()));
        LedgerEntry last = wallet.getLedgerEntries().getLast();
        assertEquals(LedgerEntryType.TRANSFER_IN, last.type());
        assertEquals(0, new BigDecimal("150.00").compareTo(last.amount()));
        assertEquals("TRANSFER-1", last.reference());
    }

    @Test
    void creditForTransferShouldThrowWhenWalletNotActive() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.deactivate(WalletStatus.CLOSED);

        assertThrows(com.aegis.wallet.domain.exception.WalletOperationNotAllowedException.class,
                () -> wallet.creditForTransfer(new BigDecimal("10.00"), "TRANSFER-1", null));
    }

    @Test
    void creditForTransferShouldRejectNonPositiveAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(IllegalArgumentException.class,
                () -> wallet.creditForTransfer(BigDecimal.ZERO, "TRANSFER-1", null));
    }

    // --- debitForPayment tests ---

    @Test
    void debitForPaymentShouldReduceBalanceAndAddPaymentEntry() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "bank", "DEP-1", "Deposit");

        wallet.debitForPayment(new BigDecimal("25.00"), "PAY-1", "Coffee");

        assertEquals(0, new BigDecimal("75.00").compareTo(wallet.getBalance()));
        LedgerEntry last = wallet.getLedgerEntries().getLast();
        assertEquals(LedgerEntryType.PAYMENT, last.type());
        assertEquals(0, new BigDecimal("25.00").compareTo(last.amount()));
        assertEquals("PAY-1", last.reference());
    }

    @Test
    void debitForPaymentShouldThrowOnInsufficientFunds() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("10.00"), "bank", "DEP-1", "Deposit");

        assertThrows(InsufficientFundsException.class,
                () -> wallet.debitForPayment(new BigDecimal("25.00"), "PAY-1", null));
    }

    @Test
    void debitForPaymentShouldThrowWhenWalletNotActive() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.deactivate(WalletStatus.CLOSED);

        assertThrows(com.aegis.wallet.domain.exception.WalletOperationNotAllowedException.class,
                () -> wallet.debitForPayment(new BigDecimal("10.00"), "PAY-1", null));
    }

    @Test
    void debitForPaymentShouldRejectNonPositiveAmount() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        assertThrows(IllegalArgumentException.class,
                () -> wallet.debitForPayment(BigDecimal.ZERO, "PAY-1", null));
    }
}
