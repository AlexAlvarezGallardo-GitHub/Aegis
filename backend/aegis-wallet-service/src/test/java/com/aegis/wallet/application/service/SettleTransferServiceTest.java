package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.HoldNotActiveException;
import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.model.LedgerEntryType;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.SettleTransferUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SettleTransferServiceTest {

    private InMemoryWalletRepository walletRepository;
    private InMemoryHoldRepository holdRepository;
    private SettleTransferService service;

    @BeforeEach
    void setUp() {
        walletRepository = new InMemoryWalletRepository();
        holdRepository = new InMemoryHoldRepository();
        service = new SettleTransferService(walletRepository, holdRepository);
    }

    @Nested
    @DisplayName("settle")
    class SettleTests {

        @Test
        void shouldSettleTransferAndUpdateBothBalances() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            source.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");

            walletRepository.wallets.add(source);
            walletRepository.wallets.add(dest);

            UUID transferId = UUID.randomUUID();
            Hold hold = Hold.reserve(source.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", transferId.toString());
            holdRepository.holds.add(hold);

            var command = new SettleTransferUseCase.SettleCommand(
                    transferId, hold.getId(),
                    source.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            var result = service.settle(command);

            assertEquals(transferId, result.transferId());
            assertEquals(hold.getId(), result.holdId());
            assertEquals(0, new BigDecimal("400.00").compareTo(result.sourceNewBalance()));
            assertEquals(0, new BigDecimal("100.00").compareTo(result.destNewBalance()));

            // Hold is SETTLED
            Hold settled = holdRepository.findById(hold.getId()).orElseThrow();
            assertEquals(HoldStatus.SETTLED, settled.getStatus());

            // Ledger entries
            assertTrue(source.getLedgerEntries().stream()
                    .anyMatch(e -> e.type() == LedgerEntryType.TRANSFER_OUT));
            assertTrue(dest.getLedgerEntries().stream()
                    .anyMatch(e -> e.type() == LedgerEntryType.TRANSFER_IN));
        }

        @Test
        void shouldThrowHoldNotFoundWhenHoldMissing() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(source);
            walletRepository.wallets.add(dest);

            var command = new SettleTransferUseCase.SettleCommand(
                    UUID.randomUUID(), UUID.randomUUID(),
                    source.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("10.00"), "EUR");

            assertThrows(HoldNotFoundException.class, () -> service.settle(command));
        }

        @Test
        void shouldThrowHoldNotActiveWhenHoldAlreadySettledForDifferentTransfer() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            source.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(source);
            walletRepository.wallets.add(dest);

            UUID originalTransferId = UUID.randomUUID();
            Hold hold = Hold.reserve(source.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", originalTransferId.toString());
            hold.settle();
            holdRepository.holds.add(hold);

            // Try to settle with a DIFFERENT transferId — should throw
            var command = new SettleTransferUseCase.SettleCommand(
                    UUID.randomUUID(), hold.getId(),
                    source.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            assertThrows(HoldNotActiveException.class, () -> service.settle(command));
        }

        @Test
        void shouldThrowWhenHoldReferenceDoesNotMatchTransferId() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            source.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(source);
            walletRepository.wallets.add(dest);

            Hold hold = Hold.reserve(source.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", "OTHER-REF");
            holdRepository.holds.add(hold);

            var command = new SettleTransferUseCase.SettleCommand(
                    UUID.randomUUID(), hold.getId(),
                    source.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            assertThrows(IllegalArgumentException.class, () -> service.settle(command));
        }

        @Test
        void shouldThrowWalletNotActiveWhenSourceFrozen() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            // Rehydrate as FROZEN with balance
            Wallet frozenSource = Wallet.rehydrate(
                    source.getWalletId(), source.getUserId(), new BigDecimal("500.00"),
                    "EUR", WalletStatus.FROZEN,
                    source.getCreatedAt(), source.getUpdatedAt(), source.getVersion(),
                    source.getLedgerEntries());
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(frozenSource);
            walletRepository.wallets.add(dest);

            UUID transferId = UUID.randomUUID();
            Hold hold = Hold.reserve(frozenSource.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", transferId.toString());
            holdRepository.holds.add(hold);

            var command = new SettleTransferUseCase.SettleCommand(
                    transferId, hold.getId(),
                    frozenSource.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            assertThrows(WalletNotActiveException.class, () -> service.settle(command));
        }

        @Test
        void shouldThrowCurrencyMismatchWhenSourceCurrencyDiffers() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            source.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            Wallet dest = Wallet.create(UUID.randomUUID(), "USD");
            walletRepository.wallets.add(source);
            walletRepository.wallets.add(dest);

            UUID transferId = UUID.randomUUID();
            Hold hold = Hold.reserve(source.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", transferId.toString());
            holdRepository.holds.add(hold);

            var command = new SettleTransferUseCase.SettleCommand(
                    transferId, hold.getId(),
                    source.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            assertThrows(CurrencyMismatchException.class, () -> service.settle(command));
        }

        @Test
        void shouldThrowWalletNotFoundWhenSourceMissing() {
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(dest);

            UUID transferId = UUID.randomUUID();
            Hold hold = Hold.reserve(UUID.randomUUID(),
                    new BigDecimal("100.00"), "EUR", transferId.toString());
            holdRepository.holds.add(hold);

            var command = new SettleTransferUseCase.SettleCommand(
                    transferId, hold.getId(),
                    UUID.randomUUID(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            assertThrows(WalletNotFoundException.class, () -> service.settle(command));
        }

        @Test
        void shouldLockWalletsInDeterministicOrder() {
            // Use UUIDs that compare correctly with signed comparison
            // MSB must be positive for both
            UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID high = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");

            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            source.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");

            // Replace walletIds via rehydrate to force ordering
            Wallet sourceHigh = Wallet.rehydrate(
                    WalletId.of(high), source.getUserId(), source.getBalance(),
                    source.getCurrency(), source.getStatus(),
                    source.getCreatedAt(), source.getUpdatedAt(), source.getVersion(),
                    source.getLedgerEntries());
            Wallet destLow = Wallet.rehydrate(
                    WalletId.of(low), dest.getUserId(), dest.getBalance(),
                    dest.getCurrency(), dest.getStatus(),
                    dest.getCreatedAt(), dest.getUpdatedAt(), dest.getVersion(),
                    dest.getLedgerEntries());

            walletRepository.wallets.add(sourceHigh);
            walletRepository.wallets.add(destLow);

            UUID transferId = UUID.randomUUID();
            Hold hold = Hold.reserve(sourceHigh.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", transferId.toString());
            holdRepository.holds.add(hold);

            var command = new SettleTransferUseCase.SettleCommand(
                    transferId, hold.getId(),
                    sourceHigh.getWalletId().value(), destLow.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            var result = service.settle(command);

            // Lock order recorded: low first, then high
            assertEquals(low, walletRepository.lockOrder.getFirst());
            assertEquals(high, walletRepository.lockOrder.get(1));

            assertEquals(0, new BigDecimal("400.00").compareTo(result.sourceNewBalance()));
            assertEquals(0, new BigDecimal("100.00").compareTo(result.destNewBalance()));
        }

        @Test
        void shouldBeIdempotentWhenHoldAlreadySettledForSameTransfer() {
            Wallet source = Wallet.create(UUID.randomUUID(), "EUR");
            source.depositFunds(new BigDecimal("500.00"), "SRC", "REF-0", null);
            Wallet dest = Wallet.create(UUID.randomUUID(), "EUR");
            walletRepository.wallets.add(source);
            walletRepository.wallets.add(dest);

            UUID transferId = UUID.randomUUID();
            Hold hold = Hold.reserve(source.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR", transferId.toString());
            hold.settle();
            holdRepository.holds.add(hold);

            int initialLedgerSize = source.getLedgerEntries().size();

            var command = new SettleTransferUseCase.SettleCommand(
                    transferId, hold.getId(),
                    source.getWalletId().value(), dest.getWalletId().value(),
                    new BigDecimal("100.00"), "EUR");

            var result = service.settle(command);

            assertNotNull(result);
            // No new ledger entries should have been appended
            assertEquals(initialLedgerSize, source.getLedgerEntries().size());
        }
    }

    // --- In-memory stubs ---

    private static class InMemoryWalletRepository implements WalletRepository {
        final List<Wallet> wallets = new ArrayList<>();
        final List<UUID> lockOrder = new ArrayList<>();

        @Override public Wallet save(Wallet wallet) {
            // Replace in list if present
            wallets.removeIf(w -> w.getWalletId().equals(wallet.getWalletId()));
            wallets.add(wallet);
            return wallet;
        }

        @Override public Optional<Wallet> findById(WalletId walletId) {
            return wallets.stream().filter(w -> w.getWalletId().equals(walletId)).findFirst();
        }

        @Override public Optional<Wallet> findByIdForUpdate(WalletId walletId) {
            lockOrder.add(walletId.value());
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
