package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletQueryServiceTest {

    private StubWalletRepository walletRepository;
    private WalletQueryService service;

    @BeforeEach
    void setUp() {
        walletRepository = new StubWalletRepository();
        service = new WalletQueryService(walletRepository);
    }

    @Test
    void listByUserShouldReturnWalletsForUser() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.create(userId, "EUR");
        walletRepository.wallets.add(wallet);

        var results = service.listByUser(userId);

        assertEquals(1, results.size());
        assertEquals(wallet.getWalletId().value(), results.getFirst().walletId());
    }

    @Test
    void listByUserShouldReturnEmptyListWhenNoWallets() {
        var results = service.listByUser(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void getDetailShouldReturnWalletDetails() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.create(userId, "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "SRC", "REF-0", null);
        walletRepository.wallets.add(wallet);

        var result = service.getDetail(wallet.getWalletId().value(), userId);

        assertEquals(wallet.getWalletId().value(), result.walletId());
        assertEquals(userId, result.userId());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.balance()));
        assertEquals("EUR", result.currency());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void getDetailShouldThrowWhenWalletNotFound() {
        assertThrows(WalletNotFoundException.class,
                () -> service.getDetail(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void getDetailShouldThrowWhenUserIdDoesNotMatch() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        walletRepository.wallets.add(wallet);

        assertThrows(WalletNotFoundException.class,
                () -> service.getDetail(wallet.getWalletId().value(), UUID.randomUUID()));
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
}
