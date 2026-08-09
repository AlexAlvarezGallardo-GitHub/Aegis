package com.aegis.reporting.infrastructure.messaging;

import com.aegis.reporting.application.service.BalanceProjectionService;
import com.aegis.reporting.domain.event.FundsDepositedEvent;
import com.aegis.reporting.domain.model.BalanceProjection;
import com.aegis.reporting.infrastructure.persistence.ProcessedEventJpaRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FundsDepositedConsumer - Kafka Consumer")
class FundsDepositedConsumerTest {

    @Mock
    private BalanceProjectionService balanceProjectionService;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    private FundsDepositedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new FundsDepositedConsumer(balanceProjectionService, processedEventRepository);
    }

    @Test
    @DisplayName("Should create new projection when wallet not found")
    void shouldCreateNewProjectionWhenNotFound() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        FundsDepositedEvent event = new FundsDepositedEvent(
                eventId, "FUNDS_DEPOSITED", "1.0",
                walletId, userId, new BigDecimal("100.00"), "USD",
                "BANK_TRANSFER", "REF-001", new BigDecimal("500.00"),
                timestamp, "corr-123"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(balanceProjectionService.findByWalletId(walletId)).thenReturn(Optional.empty());
        when(balanceProjectionService.save(any(BalanceProjection.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "wallet.funds.deposited", 0, 5L);

        // Assert
        ArgumentCaptor<BalanceProjection> captor = ArgumentCaptor.forClass(BalanceProjection.class);
        verify(balanceProjectionService).save(captor.capture());

        BalanceProjection saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(walletId, saved.walletId());
        assertEquals(userId, saved.userId());
        assertEquals(new BigDecimal("500.00"), saved.balance());
        assertEquals("USD", saved.currency());
        assertEquals(timestamp, saved.lastUpdated());
    }

    @Test
    @DisplayName("Should update existing projection when wallet found")
    void shouldUpdateExistingProjection() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        Instant oldTimestamp = Instant.now().minusSeconds(60);
        Instant newTimestamp = Instant.now();

        BalanceProjection existing = new BalanceProjection(existingId, walletId, userId,
                new BigDecimal("500.00"), "USD", oldTimestamp);

        FundsDepositedEvent event = new FundsDepositedEvent(
                eventId, "FUNDS_DEPOSITED", "1.0",
                walletId, userId, new BigDecimal("100.00"), "USD",
                "BANK_TRANSFER", "REF-001", new BigDecimal("600.00"),
                newTimestamp, "corr-456"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(balanceProjectionService.findByWalletId(walletId)).thenReturn(Optional.of(existing));
        when(balanceProjectionService.save(any(BalanceProjection.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "wallet.funds.deposited", 0, 5L);

        // Assert
        ArgumentCaptor<BalanceProjection> captor = ArgumentCaptor.forClass(BalanceProjection.class);
        verify(balanceProjectionService).save(captor.capture());

        BalanceProjection saved = captor.getValue();
        assertEquals(existingId, saved.id());
        assertEquals(walletId, saved.walletId());
        assertEquals(new BigDecimal("600.00"), saved.balance());
        assertEquals(newTimestamp, saved.lastUpdated());
    }

    @Test
    @DisplayName("Should skip already-processed events without updating the projection")
    void shouldSkipDuplicateEvents() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        FundsDepositedEvent event = new FundsDepositedEvent(
                eventId, "FUNDS_DEPOSITED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "USD",
                "BANK_TRANSFER", "REF-DUP", new BigDecimal("600.00"), Instant.now(), "corr-dup"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(0);

        // Act
        consumer.consume(event, "wallet.funds.deposited", 1, 9L);

        // Assert - duplicate must not update the projection
        verify(balanceProjectionService, never()).save(any(BalanceProjection.class));
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("wallet.funds.deposited"),
                eq(1), eq(9L), any());
    }
}
