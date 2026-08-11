package com.aegis.wallet.infrastructure.persistence;

import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldRepositoryAdapterTest {

    @Mock
    private HoldJpaRepository holdJpaRepository;

    private HoldRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new HoldRepositoryAdapter(holdJpaRepository);
    }

    @Test
    void saveShouldPersistAndReturnDomainHold() {
        Hold hold = Hold.reserve(UUID.randomUUID(), new BigDecimal("100.00"), "EUR", "TXN-1");

        HoldJpaEntity entity = new HoldJpaEntity(
                hold.getId(), hold.getWalletId(), hold.getAmount(), hold.getCurrency(),
                hold.getReference(), hold.getStatus().name(),
                hold.getCreatedAt(), hold.getExpiresAt());

        when(holdJpaRepository.save(any(HoldJpaEntity.class))).thenReturn(entity);

        Hold saved = adapter.save(hold);

        verify(holdJpaRepository).save(any(HoldJpaEntity.class));
        assertEquals(hold.getId(), saved.getId());
        assertEquals(HoldStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void findByIdShouldReturnEmptyWhenNotFound() {
        when(holdJpaRepository.findById(any())).thenReturn(Optional.empty());
        assertTrue(adapter.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByIdShouldReturnHoldWhenFound() {
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Instant now = Instant.now();
        HoldJpaEntity entity = new HoldJpaEntity(
                id, walletId, new BigDecimal("50.00"), "EUR",
                "REF-1", "ACTIVE", now, now.plusSeconds(300));

        when(holdJpaRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Hold> result = adapter.findById(id);
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(HoldStatus.ACTIVE, result.get().getStatus());
    }

    @Test
    void findActiveByReferenceShouldReturnEmptyWhenNoMatch() {
        when(holdJpaRepository.findActiveByReference("MISSING")).thenReturn(Optional.empty());
        assertTrue(adapter.findActiveByReference("MISSING").isEmpty());
    }

    @Test
    void sumActiveAmountShouldReturnZeroWhenNull() {
        when(holdJpaRepository.sumActiveAmountByWalletId(any())).thenReturn(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(
                adapter.sumActiveAmountByWalletId(UUID.randomUUID())));
    }

    @Test
    void sumActiveAmountShouldReturnValueFromQuery() {
        UUID walletId = UUID.randomUUID();
        when(holdJpaRepository.sumActiveAmountByWalletId(walletId))
                .thenReturn(new BigDecimal("250.00"));
        assertEquals(0, new BigDecimal("250.00").compareTo(
                adapter.sumActiveAmountByWalletId(walletId)));
    }
}
