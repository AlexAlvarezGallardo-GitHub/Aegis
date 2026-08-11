package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.HoldNotActiveException;
import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.ReleaseHoldUseCase;
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

class ReleaseHoldServiceTest {

    private InMemoryWalletRepository walletRepository;
    private InMemoryHoldRepository holdRepository;
    private ReleaseHoldService service;

    @BeforeEach
    void setUp() {
        walletRepository = new InMemoryWalletRepository();
        holdRepository = new InMemoryHoldRepository();
        service = new ReleaseHoldService(walletRepository, holdRepository);
    }

    @Nested
    @DisplayName("release")
    class ReleaseTests {

        @Test
        void shouldReleaseActiveHold() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            Hold hold = Hold.reserve(wallet.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", "TXN-1");
            holdRepository.holds.add(hold);

            var command = new ReleaseHoldUseCase.ReleaseCommand(
                    wallet.getWalletId().value(), hold.getId());

            var result = service.release(command);

            assertEquals(hold.getId(), result.holdId());
            assertEquals("RELEASED", result.status());
            assertEquals(0, new BigDecimal("500.00").compareTo(result.availableBalance()));
        }

        @Test
        void shouldBeIdempotentWhenAlreadyReleased() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            Hold hold = Hold.reserve(wallet.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", "TXN-2");
            hold.release();
            holdRepository.holds.add(hold);

            var command = new ReleaseHoldUseCase.ReleaseCommand(
                    wallet.getWalletId().value(), hold.getId());

            var result = service.release(command);

            assertEquals("RELEASED", result.status());
        }

        @Test
        void shouldThrowHoldNotActiveWhenHoldSettled() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            wallet.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            walletRepository.wallets.add(wallet);

            Hold hold = Hold.reserve(wallet.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", "TXN-3");
            hold.settle();
            holdRepository.holds.add(hold);

            var command = new ReleaseHoldUseCase.ReleaseCommand(
                    wallet.getWalletId().value(), hold.getId());

            assertThrows(HoldNotActiveException.class, () -> service.release(command));
        }

        @Test
        void shouldThrowHoldNotFoundWhenHoldMissing() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(wallet);

            var command = new ReleaseHoldUseCase.ReleaseCommand(
                    wallet.getWalletId().value(), UUID.randomUUID());

            assertThrows(HoldNotFoundException.class, () -> service.release(command));
        }

        @Test
        void shouldThrowHoldNotFoundWhenWalletIdDoesNotMatch() {
            Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(wallet);

            Hold hold = Hold.reserve(wallet.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", "TXN-4");
            holdRepository.holds.add(hold);

            var command = new ReleaseHoldUseCase.ReleaseCommand(
                    UUID.randomUUID(), hold.getId());

            assertThrows(HoldNotFoundException.class, () -> service.release(command));
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
