package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
@DisplayName("TransferRepositoryAdapter - Persistence Adapter")
class TransferRepositoryAdapterTest {

    @Mock
    private TransferJpaRepository jpaRepository;

    @InjectMocks
    private TransferRepositoryAdapter adapter;

    @Test
    @DisplayName("Should save domain transfer via JPA entity")
    void shouldSave() {
        Transfer transfer = Transfer.request(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("100.00"), "EUR", null, "ref-001");
        when(jpaRepository.save(any(TransferJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Transfer result = adapter.save(transfer);

        assertEquals(transfer.getId(), result.getId());
        verify(jpaRepository).save(any(TransferJpaEntity.class));
    }

    @Test
    @DisplayName("Should find transfer by ID and map to domain")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID dest = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Instant now = Instant.now();

        Transfer domain = Transfer.rehydrate(id, source, dest, user,
                new BigDecimal("100.00"), "EUR", null, "ref-001",
                TransferStatus.PENDING, null, null, null, now, now, null);
        TransferJpaEntity entity = new TransferJpaEntity(domain);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<Transfer> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(TransferStatus.PENDING, result.get().getStatus());
        assertEquals(source, result.get().getSourceWalletId());
    }

    @Test
    @DisplayName("Should return empty when not found")
    void shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Transfer> result = adapter.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should check existence by source wallet and reference")
    void shouldCheckExistence() {
        UUID source = UUID.randomUUID();
        when(jpaRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(true);

        assertTrue(adapter.existsBySourceWalletIdAndReference(source, "ref-001"));
    }

    @Test
    @DisplayName("Should return false when no duplicate exists")
    void shouldReturnFalseWhenNoDuplicate() {
        UUID source = UUID.randomUUID();
        when(jpaRepository.existsBySourceWalletIdAndReference(source, "ref-new")).thenReturn(false);

        assertFalse(adapter.existsBySourceWalletIdAndReference(source, "ref-new"));
    }
}
