package com.aegis.wallet.infrastructure.persistence;

import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletRepositoryAdapterTest {

    @Mock
    private WalletJpaRepository walletJpaRepository;
    @Mock
    private LedgerEntryJpaRepository ledgerEntryJpaRepository;

    private WalletRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WalletRepositoryAdapter(walletJpaRepository, ledgerEntryJpaRepository);
    }

    @Test
    void saveShouldPersistWalletAndLedgerEntries() {
        Wallet wallet = Wallet.create(UUID.randomUUID(), "EUR");
        wallet.depositFunds(new BigDecimal("100.00"), "SRC", "REF-1", null);

        WalletJpaEntity entity = new WalletJpaEntity(
                wallet.getWalletId().value(), wallet.getUserId(), wallet.getBalance(),
                wallet.getCurrency(), wallet.getStatus().name(),
                wallet.getCreatedAt(), wallet.getUpdatedAt(), wallet.getVersion());

        when(walletJpaRepository.save(any(WalletJpaEntity.class))).thenReturn(entity);
        when(ledgerEntryJpaRepository.findByWalletIdOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());

        Wallet saved = adapter.save(wallet);

        verify(walletJpaRepository).save(any(WalletJpaEntity.class));
        assertNotNull(saved);
    }

    @Test
    void findByIdShouldReturnEmptyWhenNotFound() {
        when(walletJpaRepository.findById(any())).thenReturn(Optional.empty());
        assertTrue(adapter.findById(WalletId.generate()).isEmpty());
    }

    @Test
    void findByIdShouldReturnWalletWhenFound() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        WalletJpaEntity entity = new WalletJpaEntity(
                id, userId, BigDecimal.ZERO, "EUR", "ACTIVE", now, now, 0L);

        when(walletJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(ledgerEntryJpaRepository.findByWalletIdOrderByCreatedAtAsc(id))
                .thenReturn(List.of());

        Optional<Wallet> result = adapter.findById(WalletId.of(id));
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getWalletId().value());
    }

    @Test
    void findByIdForUpdateShouldReturnEmptyWhenNotFound() {
        when(walletJpaRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());
        assertTrue(adapter.findByIdForUpdate(WalletId.generate()).isEmpty());
    }

    @Test
    void findByIdForUpdateShouldReturnWalletWhenFound() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        WalletJpaEntity entity = new WalletJpaEntity(
                id, userId, new BigDecimal("50.00"), "USD", "ACTIVE", now, now, 1L);

        when(walletJpaRepository.findByIdForUpdate(id)).thenReturn(Optional.of(entity));
        when(ledgerEntryJpaRepository.findByWalletIdOrderByCreatedAtAsc(id))
                .thenReturn(List.of());

        Optional<Wallet> result = adapter.findByIdForUpdate(WalletId.of(id));
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("50.00").compareTo(result.get().getBalance()));
    }

    @Test
    void findByUserIdShouldReturnEmptyListWhenNoWallets() {
        UUID userId = UUID.randomUUID();
        when(walletJpaRepository.findByUserId(userId)).thenReturn(List.of());
        assertTrue(adapter.findByUserId(userId).isEmpty());
    }

    @Test
    void countByUserIdShouldReturnCount() {
        UUID userId = UUID.randomUUID();
        when(walletJpaRepository.countByUserId(userId)).thenReturn(5L);
        assertEquals(5L, adapter.countByUserId(userId));
    }
}
