package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.event.WalletBalanceAdjusted;
import com.aegis.wallet.domain.event.WalletCreated;
import com.aegis.wallet.domain.event.FundsDeposited;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.UpdateWalletUseCase;
import com.aegis.wallet.domain.port.outbound.EventPublisher;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UpdateWalletServiceTest {

    private StubWalletRepository walletRepository;
    private StubEventPublisher eventPublisher;
    private UpdateWalletService service;

    @BeforeEach
    void setUp() {
        walletRepository = new StubWalletRepository();
        eventPublisher = new StubEventPublisher();
        service = new UpdateWalletService(walletRepository, eventPublisher);
    }

    @Test
    void adjustBalanceShouldUpdateBalanceAndPublishEvent() {
        Wallet wallet = Wallet.create(userId(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "SRC", "REF-0", null);
        walletRepository.wallets.add(wallet);

        var command = new UpdateWalletUseCase.AdjustBalanceCommand(
                wallet.getWalletId().value(), wallet.getUserId(),
                new BigDecimal("-50.00"), "Withdrawal", "corr-1");

        var result = service.adjustBalance(command);

        assertEquals(0, new BigDecimal("50.00").compareTo(result.balance()));
        assertEquals(1, eventPublisher.balanceAdjustedEvents.size());
    }

    @Test
    void adjustBalanceShouldThrowWhenWalletNotFound() {
        var command = new UpdateWalletUseCase.AdjustBalanceCommand(
                UUID.randomUUID(), userId(),
                new BigDecimal("10.00"), "Test", "corr-2");

        assertThrows(WalletNotFoundException.class, () -> service.adjustBalance(command));
    }

    @Test
    void changeStatusShouldDeactivateWallet() {
        Wallet wallet = Wallet.create(userId(), "EUR");
        walletRepository.wallets.add(wallet);

        var command = new UpdateWalletUseCase.StatusChangeCommand(
                wallet.getWalletId().value(), wallet.getUserId(), WalletStatus.FROZEN);

        var result = service.changeStatus(command);

        assertEquals("FROZEN", result.status());
    }

    @Test
    void changeStatusShouldThrowWhenWalletNotFound() {
        var command = new UpdateWalletUseCase.StatusChangeCommand(
                UUID.randomUUID(), userId(), WalletStatus.CLOSED);

        assertThrows(WalletNotFoundException.class, () -> service.changeStatus(command));
    }

    private static UUID userId() {
        return UUID.randomUUID();
    }

    private static class StubWalletRepository implements WalletRepository {
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

    private static class StubEventPublisher implements EventPublisher {
        final List<WalletBalanceAdjusted> balanceAdjustedEvents = new ArrayList<>();

        @Override public void publish(WalletCreated event) {}
        @Override public void publish(FundsDeposited event) {}
        @Override public void publish(WalletBalanceAdjusted event) {
            balanceAdjustedEvents.add(event);
        }
    }
}
