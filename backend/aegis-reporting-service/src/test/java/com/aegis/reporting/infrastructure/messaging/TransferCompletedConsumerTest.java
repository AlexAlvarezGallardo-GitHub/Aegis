package com.aegis.reporting.infrastructure.messaging;

import com.aegis.reporting.application.service.TransferProjectionService;
import com.aegis.reporting.domain.event.TransferCompletedEvent;
import com.aegis.reporting.domain.model.TransferProjection;
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
@DisplayName("TransferCompletedConsumer - Kafka Consumer")
class TransferCompletedConsumerTest {

    @Mock
    private TransferProjectionService transferProjectionService;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    private TransferCompletedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransferCompletedConsumer(transferProjectionService, processedEventRepository);
    }

    @Test
    @DisplayName("Should create new projection when transfer not found")
    void shouldCreateNewProjectionWhenNotFound() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        TransferCompletedEvent event = new TransferCompletedEvent(
                eventId, "TRANSFER_COMPLETED", "1.0",
                transferId, sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "REF-001",
                timestamp, "corr-123",
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("400.00"), new BigDecimal("600.00")
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(transferProjectionService.findByTransferId(transferId)).thenReturn(Optional.empty());
        when(transferProjectionService.save(any(TransferProjection.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.transfer.completed", 0, 5L);

        // Assert
        ArgumentCaptor<TransferProjection> captor = ArgumentCaptor.forClass(TransferProjection.class);
        verify(transferProjectionService).save(captor.capture());

        TransferProjection saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(transferId, saved.transferId());
        assertEquals(sourceWalletId, saved.sourceWalletId());
        assertEquals(destWalletId, saved.destWalletId());
        assertEquals(userId, saved.userId());
        assertEquals(new BigDecimal("100.00"), saved.amount());
        assertEquals("USD", saved.currency());
        assertEquals("COMPLETED", saved.status());
        assertNull(saved.failureReason());
        assertEquals(timestamp, saved.eventTimestamp());
    }

    @Test
    @DisplayName("Should update existing projection when transfer found")
    void shouldUpdateExistingProjection() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant oldTimestamp = Instant.now().minusSeconds(60);
        Instant newTimestamp = Instant.now();

        TransferProjection existing = new TransferProjection(
                existingId, transferId, sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "REQUESTED", null, oldTimestamp
        );

        TransferCompletedEvent event = new TransferCompletedEvent(
                eventId, "TRANSFER_COMPLETED", "1.0",
                transferId, sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "REF-001",
                newTimestamp, "corr-456",
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("400.00"), new BigDecimal("600.00")
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(transferProjectionService.findByTransferId(transferId)).thenReturn(Optional.of(existing));
        when(transferProjectionService.save(any(TransferProjection.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.transfer.completed", 0, 5L);

        // Assert
        ArgumentCaptor<TransferProjection> captor = ArgumentCaptor.forClass(TransferProjection.class);
        verify(transferProjectionService).save(captor.capture());

        TransferProjection saved = captor.getValue();
        assertEquals(existingId, saved.id());
        assertEquals(transferId, saved.transferId());
        assertEquals("COMPLETED", saved.status());
        assertNull(saved.failureReason());
        assertEquals(newTimestamp, saved.eventTimestamp());
    }

    @Test
    @DisplayName("Should skip already-processed events without updating the projection")
    void shouldSkipDuplicateEvents() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        TransferCompletedEvent event = new TransferCompletedEvent(
                eventId, "TRANSFER_COMPLETED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "USD", "REF-DUP",
                Instant.now(), "corr-dup",
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("400.00"), new BigDecimal("600.00")
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(0);

        // Act
        consumer.consume(event, "payment.transfer.completed", 1, 9L);

        // Assert - duplicate must not update the projection
        verify(transferProjectionService, never()).save(any(TransferProjection.class));
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.transfer.completed"),
                eq(1), eq(9L), any());
    }
}
