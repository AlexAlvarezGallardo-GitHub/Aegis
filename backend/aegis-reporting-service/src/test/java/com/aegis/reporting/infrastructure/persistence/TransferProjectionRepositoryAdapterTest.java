package com.aegis.reporting.infrastructure.persistence;

import com.aegis.reporting.domain.model.TransferProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferProjectionRepositoryAdapter - Persistence Adapter")
class TransferProjectionRepositoryAdapterTest {

    @Mock
    private TransferProjectionJpaRepository jpaRepository;

    private TransferProjectionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TransferProjectionRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        TransferProjection projection = new TransferProjection(
                id, transferId, sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "COMPLETED", null, now
        );

        when(jpaRepository.save(any(TransferProjectionJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TransferProjection saved = adapter.save(projection);

        // Assert
        ArgumentCaptor<TransferProjectionJpaEntity> captor = ArgumentCaptor.forClass(TransferProjectionJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        TransferProjectionJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(transferId, entity.getTransferId());
        assertEquals(sourceWalletId, entity.getSourceWalletId());
        assertEquals(destWalletId, entity.getDestWalletId());
        assertEquals(userId, entity.getUserId());
        assertEquals(new BigDecimal("100.00"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals("COMPLETED", entity.getStatus());
        assertNull(entity.getFailureReason());
        assertEquals(now, entity.getEventTimestamp());

        assertNotNull(saved);
        assertEquals(id, saved.id());
    }

    @Test
    @DisplayName("Should find by transferId and convert to domain model")
    void shouldFindByTransferId() {
        // Arrange
        UUID transferId = UUID.randomUUID();
        TransferProjectionJpaEntity entity = new TransferProjectionJpaEntity(
                UUID.randomUUID(), transferId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500.00"), "EUR", "COMPLETED", null, Instant.now()
        );

        when(jpaRepository.findByTransferId(transferId)).thenReturn(Optional.of(entity));

        // Act
        Optional<TransferProjection> result = adapter.findByTransferId(transferId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(transferId, result.get().transferId());
        assertEquals(new BigDecimal("500.00"), result.get().amount());
        assertEquals("EUR", result.get().currency());
    }

    @Test
    @DisplayName("Should return empty when transferId not found")
    void shouldReturnEmptyWhenNotFound() {
        // Arrange
        UUID transferId = UUID.randomUUID();
        when(jpaRepository.findByTransferId(transferId)).thenReturn(Optional.empty());

        // Act
        Optional<TransferProjection> result = adapter.findByTransferId(transferId);

        // Assert
        assertTrue(result.isEmpty());
        verify(jpaRepository).findByTransferId(transferId);
    }
}
