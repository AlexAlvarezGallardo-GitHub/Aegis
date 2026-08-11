package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.DepositReversalException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.LedgerEntry;
import com.aegis.wallet.domain.model.LedgerEntryType;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.ReverseDepositUseCase;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReverseDepositServiceTest {

    private StubWalletRepository walletRepository;
    private ReverseDepositService service;

    @BeforeEach
    void setUp() {
        walletRepository = new StubWalletRepository();
        service = new ReverseDepositService(walletRepository);
    }

    @Test
    void reverseShouldReduceBalanceAndReturnReceipt() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "BANK_TRANSFER", "TXN-001", null);
        UUID depositId = wallet.getLedgerEntries().get(1).id();
        walletRepository.wallets.add(wallet);

        var command = new ReverseDepositUseCase.ReverseCommand(
                wallet.getWalletId().value(), wallet.getUserId(), depositId, "REV-001", "corr-1");

        var result = service.reverse(command);

        assertNotNull(result.reversalId());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.newBalance()));
        assertEquals(0, new BigDecimal("100.00").compareTo(result.reversedAmount()));
        assertEquals("EUR", result.currency());
    }

    @Test
    void reverseShouldRejectNonExistentWallet() {
        var command = new ReverseDepositUseCase.ReverseCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "REV-002", "corr-2");
        assertThrows(WalletNotFoundException.class, () -> service.reverse(command));
    }

    @Test
    void reverseShouldRejectWrongOwnership() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        walletRepository.wallets.add(wallet);

        var command = new ReverseDepositUseCase.ReverseCommand(
                wallet.getWalletId().value(), UUID.randomUUID(), UUID.randomUUID(), "REV-003", "corr-3");
        assertThrows(WalletNotFoundException.class, () -> service.reverse(command));
    }

    @Test
    void reverseShouldRejectUnknownDeposit() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        walletRepository.wallets.add(wallet);

        var command = new ReverseDepositUseCase.ReverseCommand(
                wallet.getWalletId().value(), wallet.getUserId(), UUID.randomUUID(), "REV-004", "corr-4");
        assertThrows(DepositReversalException.class, () -> service.reverse(command));
    }

    private static class StubWalletRepository implements WalletRepository {
        final List<Wallet> wallets = new ArrayList<>();

        @Override
        public Wallet save(Wallet wallet) {
            return wallet;
        }

        @Override
        public Optional<Wallet> findById(WalletId walletId) {
            return wallets.stream()
                    .filter(w -> w.getWalletId().equals(walletId))
                    .findFirst();
        }

        @Override
        public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
            return findById(walletId);
        }

        @Override
        public List<Wallet> findByUserId(UUID userId) {
            return wallets.stream()
                    .filter(w -> w.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public long countByUserId(UUID userId) {
            return wallets.size();
        }
    }
}
