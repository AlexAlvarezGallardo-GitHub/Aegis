package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.TransferAuditRecordService;
import com.aegis.audit.domain.event.TransferFailedEvent;
import com.aegis.audit.domain.model.TransferAuditRecord;
import com.aegis.audit.infrastructure.persistence.ProcessedEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferFailedConsumer - Kafka Consumer")
class TransferFailedConsumerTest {

    @Mock
    private TransferAuditRecordService transferAuditRecordService;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    private TransferFailedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransferFailedConsumer(transferAuditRecordService, processedEventRepository);
    }

    @Test
    @DisplayName("Should map event to TransferAuditRecord and persist via service")
    void shouldMapAndPersistTransferAuditRecord() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        TransferFailedEvent event = new TransferFailedEvent(
                eventId, "TRANSFER_FAILED", "1.0",
                transferId, sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "REF-001",
                timestamp, "corr-123",
                "INSUFFICIENT_FUNDS", "Wallet balance too low", true
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(transferAuditRecordService.save(any(TransferAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.transfer.failed", 0, 7L);

        // Assert
        ArgumentCaptor<TransferAuditRecord> captor = ArgumentCaptor.forClass(TransferAuditRecord.class);
        verify(transferAuditRecordService).save(captor.capture());

        TransferAuditRecord saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(eventId, saved.eventId());
        assertEquals(transferId, saved.transferId());
        assertEquals("FAILED", saved.eventType());
        assertEquals(sourceWalletId, saved.sourceWalletId());
        assertEquals(destWalletId, saved.destWalletId());
        assertEquals(userId, saved.userId());
        assertEquals(new BigDecimal("100.00"), saved.amount());
        assertEquals("USD", saved.currency());
        assertEquals("REF-001", saved.reference());
        assertEquals("INSUFFICIENT_FUNDS", saved.failureReason());
        assertEquals("corr-123", saved.correlationId());
        assertEquals(timestamp, saved.eventTimestamp());
        assertNotNull(saved.ingestedAt());
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.transfer.failed"),
                eq(0), eq(7L), any());
    }

    @Test
    @DisplayName("Should handle event with null failureDetails")
    void shouldHandleNullFailureDetails() {
        // Arrange
        TransferFailedEvent event = new TransferFailedEvent(
                UUID.randomUUID(), "TRANSFER_FAILED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "EUR", null,
                Instant.now(), null,
                "TIMEOUT", null, false
        );

        when(processedEventRepository.insertIfAbsent(any(), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(transferAuditRecordService.save(any(TransferAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.transfer.failed", 0, 8L);

        // Assert
        verify(transferAuditRecordService).save(any(TransferAuditRecord.class));
    }

    @Test
    @DisplayName("Should skip already-processed events without persisting a duplicate")
    void shouldSkipDuplicateEvents() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        TransferFailedEvent event = new TransferFailedEvent(
                eventId, "TRANSFER_FAILED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), "EUR", "REF-DUP",
                Instant.now(), "corr-dup",
                "REJECTED", null, false
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(0);

        // Act
        consumer.consume(event, "payment.transfer.failed", 2, 33L);

        // Assert - duplicate must not be persisted
        verify(transferAuditRecordService, never()).save(any(TransferAuditRecord.class));
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.transfer.failed"),
                eq(2), eq(33L), any());
    }
}
