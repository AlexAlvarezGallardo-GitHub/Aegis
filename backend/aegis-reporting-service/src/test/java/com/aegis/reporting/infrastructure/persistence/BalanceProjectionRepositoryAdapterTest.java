package com.aegis.reporting.infrastructure.persistence;

import com.aegis.reporting.domain.model.BalanceProjection;
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
@DisplayName("BalanceProjectionRepositoryAdapter - Persistence Adapter")
class BalanceProjectionRepositoryAdapterTest {

    @Mock
    private BalanceProjectionJpaRepository jpaRepository;

    private BalanceProjectionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BalanceProjectionRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        BalanceProjection projection = new BalanceProjection(id, walletId, userId,
                new BigDecimal("1000.00"), "USD", now);

        when(jpaRepository.save(any(BalanceProjectionJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        BalanceProjection saved = adapter.save(projection);

        // Assert
        ArgumentCaptor<BalanceProjectionJpaEntity> captor = ArgumentCaptor.forClass(BalanceProjectionJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        BalanceProjectionJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(walletId, entity.getWalletId());
        assertEquals(userId, entity.getUserId());
        assertEquals(new BigDecimal("1000.00"), entity.getBalance());
        assertEquals("USD", entity.getCurrency());

        assertNotNull(saved);
        assertEquals(id, saved.id());
    }

    @Test
    @DisplayName("Should find by walletId and convert to domain model")
    void shouldFindByWalletId() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BalanceProjectionJpaEntity entity = new BalanceProjectionJpaEntity(
                UUID.randomUUID(), walletId, UUID.randomUUID(),
                new BigDecimal("500.00"), "EUR", Instant.now()
        );

        when(jpaRepository.findByWalletId(walletId)).thenReturn(Optional.of(entity));

        // Act
        Optional<BalanceProjection> result = adapter.findByWalletId(walletId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(walletId, result.get().walletId());
        assertEquals(new BigDecimal("500.00"), result.get().balance());
        assertEquals("EUR", result.get().currency());
    }

    @Test
    @DisplayName("Should return empty when walletId not found")
    void shouldReturnEmptyWhenNotFound() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        when(jpaRepository.findByWalletId(walletId)).thenReturn(Optional.empty());

        // Act
        Optional<BalanceProjection> result = adapter.findByWalletId(walletId);

        // Assert
        assertTrue(result.isEmpty());
        verify(jpaRepository).findByWalletId(walletId);
    }
}
