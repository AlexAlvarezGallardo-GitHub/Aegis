package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.event.FundsDeposited;
import com.aegis.wallet.domain.event.WalletBalanceAdjusted;
import com.aegis.wallet.domain.event.WalletCreated;
import com.aegis.wallet.domain.exception.DuplicateDepositException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.LedgerEntry;
import com.aegis.wallet.domain.model.LedgerEntryType;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.DepositFundsUseCase;
import com.aegis.wallet.domain.port.outbound.EventPublisher;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DepositFundsServiceTest {

    private StubWalletRepository walletRepository;
    private StubEventPublisher eventPublisher;
    private DepositFundsService service;

    @BeforeEach
    void setUp() {
        walletRepository = new StubWalletRepository();
        eventPublisher = new StubEventPublisher();
        service = new DepositFundsService(walletRepository, eventPublisher);
    }

    @Test
    void depositShouldSucceed() {
        Wallet wallet = Wallet.create(userId(), "EUR");
        walletRepository.wallets.add(wallet);

        var command = new DepositFundsUseCase.DepositCommand(
                wallet.getWalletId().value(), wallet.getUserId(),
                new BigDecimal("100.00"), "EUR", "BANK_TRANSFER", "TXN-001", "corr-1");

        var result = service.deposit(command);

        assertNotNull(result.depositId());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.newBalance()));
        assertEquals("BANK_TRANSFER", result.source());
        assertEquals("TXN-001", result.reference());
        assertEquals(1, eventPublisher.fundsDepositedEvents.size());
    }

    @Test
    void depositShouldRejectDuplicateReference() {
        Wallet wallet = Wallet.create(userId(), "EUR");
        wallet.depositFunds(new BigDecimal("50.00"), "CARD", "TXN-001", null);
        walletRepository.wallets.add(wallet);

        var command = new DepositFundsUseCase.DepositCommand(
                wallet.getWalletId().value(), wallet.getUserId(),
                new BigDecimal("100.00"), "EUR", "BANK_TRANSFER", "TXN-001", "corr-2");

        assertThrows(DuplicateDepositException.class, () -> service.deposit(command));
    }

    @Test
    void depositShouldRejectNonExistentWallet() {
        var command = new DepositFundsUseCase.DepositCommand(
                UUID.randomUUID(), userId(),
                new BigDecimal("100.00"), "EUR", "BANK_TRANSFER", "TXN-002", "corr-3");

        assertThrows(WalletNotFoundException.class, () -> service.deposit(command));
    }

    @Test
    void depositShouldRejectWrongOwnership() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        walletRepository.wallets.add(wallet);

        var command = new DepositFundsUseCase.DepositCommand(
                wallet.getWalletId().value(), UUID.randomUUID(),
                new BigDecimal("100.00"), "EUR", "BANK_TRANSFER", "TXN-003", "corr-4");

        assertThrows(WalletNotFoundException.class, () -> service.deposit(command));
    }

    @Test
    void depositShouldPublishFundsDepositedEvent() {
        Wallet wallet = Wallet.create(userId(), "EUR");
        walletRepository.wallets.add(wallet);

        var command = new DepositFundsUseCase.DepositCommand(
                wallet.getWalletId().value(), wallet.getUserId(),
                new BigDecimal("250.00"), "EUR", "CARD", "TXN-004", "corr-5");

        service.deposit(command);

        FundsDeposited event = eventPublisher.fundsDepositedEvents.getFirst();
        assertEquals(wallet.getWalletId().value(), event.walletId());
        assertEquals(wallet.getUserId(), event.userId());
        assertEquals(0, new BigDecimal("250.00").compareTo(event.amount()));
        assertEquals("EUR", event.currency());
        assertEquals("CARD", event.source());
        assertEquals("TXN-004", event.reference());
    }

    private static UUID userId() {
        return UUID.randomUUID();
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

    private static class StubEventPublisher implements EventPublisher {
        final List<FundsDeposited> fundsDepositedEvents = new ArrayList<>();

        @Override
        public void publish(WalletCreated event) {}

        @Override
        public void publish(WalletBalanceAdjusted event) {}

        @Override
        public void publish(FundsDeposited event) {
            fundsDepositedEvents.add(event);
        }
    }
}
