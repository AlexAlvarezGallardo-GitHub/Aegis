package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.InsufficientFundsException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.CreateHoldUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateHoldServiceTest {

    private InMemoryWalletRepository walletRepository;
    private InMemoryHoldRepository holdRepository;
    private CreateHoldService service;

    @BeforeEach
    void setUp() {
        walletRepository = new InMemoryWalletRepository();
        holdRepository = new InMemoryHoldRepository();
        service = new CreateHoldService(walletRepository, holdRepository);
    }

    @Nested
    @DisplayName("createHold")
    class CreateHoldTests {

        @Test
        void shouldCreateHoldWhenSufficientAvailableBalance() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            var command = new CreateHoldUseCase.CreateHoldCommand(
                    wallet.getWalletId().value(), new BigDecimal("100.00"), "EUR", "TXN-1");

            var result = service.createHold(command);

            assertNotNull(result.holdId());
            assertEquals(wallet.getWalletId().value(), result.walletId());
            assertEquals(0, new BigDecimal("100.00").compareTo(result.amount()));
            assertEquals("EUR", result.currency());
            assertEquals("TXN-1", result.reference());
            assertEquals("ACTIVE", result.status());
            assertEquals(0, new BigDecimal("400.00").compareTo(result.availableBalance()));
            assertEquals(1, holdRepository.holds.size());
        }

        @Test
        void shouldThrowInsufficientFundsWhenAmountExceedsAvailable() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("50.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            var command = new CreateHoldUseCase.CreateHoldCommand(
                    wallet.getWalletId().value(), new BigDecimal("100.00"), "EUR", "TXN-2");

            assertThrows(InsufficientFundsException.class, () -> service.createHold(command));
        }

        @Test
        void shouldThrowInsufficientFundsWhenPreviousHoldsReduceAvailable() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("200.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            // First hold takes 150
            service.createHold(new CreateHoldUseCase.CreateHoldCommand(
                    wallet.getWalletId().value(), new BigDecimal("150.00"), "EUR", "TXN-A"));

            // Second hold of 100 should fail (only 50 available)
            assertThrows(InsufficientFundsException.class, () ->
                    service.createHold(new CreateHoldUseCase.CreateHoldCommand(
                            wallet.getWalletId().value(), new BigDecimal("100.00"), "EUR", "TXN-B")));
        }

        @Test
        void shouldThrowWalletNotActiveWhenWalletFrozen() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            // Rehydrate as FROZEN with balance (deactivate requires zero balance)
            Wallet frozenWallet = Wallet.rehydrate(
                    wallet.getWalletId(), wallet.getUserId(), new BigDecimal("500.00"),
                    "EUR", WalletStatus.FROZEN,
                    wallet.getCreatedAt(), wallet.getUpdatedAt(), wallet.getVersion(),
                    wallet.getLedgerEntries());
            walletRepository.wallets.add(frozenWallet);

            var command = new CreateHoldUseCase.CreateHoldCommand(
                    frozenWallet.getWalletId().value(), new BigDecimal("10.00"), "EUR", "TXN-3");

            assertThrows(WalletNotActiveException.class, () -> service.createHold(command));
        }

        @Test
        void shouldThrowCurrencyMismatchWhenCurrencyDiffers() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            var command = new CreateHoldUseCase.CreateHoldCommand(
                    wallet.getWalletId().value(), new BigDecimal("10.00"), "USD", "TXN-4");

            assertThrows(CurrencyMismatchException.class, () -> service.createHold(command));
        }

        @Test
        void shouldThrowWalletNotFoundWhenWalletMissing() {
            var command = new CreateHoldUseCase.CreateHoldCommand(
                    UUID.randomUUID(), new BigDecimal("10.00"), "EUR", "TXN-5");

            assertThrows(WalletNotFoundException.class, () -> service.createHold(command));
        }

        @Test
        void shouldBeIdempotentWhenActiveHoldWithSameReferenceExists() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            var command = new CreateHoldUseCase.CreateHoldCommand(
                    wallet.getWalletId().value(), new BigDecimal("100.00"), "EUR", "TXN-IDEM");

            var first = service.createHold(command);
            var second = service.createHold(command);

            assertEquals(first.holdId(), second.holdId());
            assertEquals(1, holdRepository.holds.size());
        }
    }

    // --- In-memory stubs ---

    private static class InMemoryWalletRepository implements WalletRepository {
        final List<Wallet> wallets = new ArrayList<>();

        @Override public Wallet save(Wallet wallet) { return wallet; }

        @Override public Optional<Wallet> findById(WalletId walletId) {
            return wallets.stream().filter(w -> w.getWalletId().equals(walletId)).findFirst();
        }

        @Override public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
            return findById(walletId);
        }

        @Override public List<Wallet> findByUserId(UUID userId) {
            return wallets.stream().filter(w -> w.getUserId().equals(userId)).toList();
        }

        @Override public long countByUserId(UUID userId) { return wallets.size(); }
    }

    private static class InMemoryHoldRepository implements HoldRepository {
        final List<Hold> holds = new ArrayList<>();

        @Override public Hold save(Hold hold) {
            holds.removeIf(h -> h.getId().equals(hold.getId()));
            holds.add(hold);
            return hold;
        }

        @Override public Optional<Hold> findById(UUID holdId) {
            return holds.stream().filter(h -> h.getId().equals(holdId)).findFirst();
        }

        @Override public Optional<Hold> findActiveByReference(String reference) {
            return holds.stream()
                    .filter(h -> h.getReference().equals(reference) && h.getStatus() == HoldStatus.ACTIVE)
                    .findFirst();
        }

        @Override public BigDecimal sumActiveAmountByWalletId(UUID walletId) {
            return holds.stream()
                    .filter(h -> h.getWalletId().equals(walletId) && h.getStatus() == HoldStatus.ACTIVE)
                    .map(Hold::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
