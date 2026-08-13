package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.RefundAuditRecordService;
import com.aegis.audit.domain.event.PaymentRefundedEvent;
import com.aegis.audit.domain.model.RefundAuditRecord;
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
@DisplayName("PaymentRefundedConsumer - Kafka Consumer")
class PaymentRefundedConsumerTest {

    @Mock
    private RefundAuditRecordService refundAuditRecordService;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    private PaymentRefundedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentRefundedConsumer(refundAuditRecordService, processedEventRepository);
    }

    @Test
    @DisplayName("Should map event to RefundAuditRecord and persist via service")
    void shouldMapAndPersistRefundAuditRecord() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        PaymentRefundedEvent event = new PaymentRefundedEvent(
                eventId, "PAYMENT_REFUNDED", "1.0",
                refundId, paymentId, walletId, userId,
                new BigDecimal("75.00"), "USD", "Defective product",
                "REF-001", new BigDecimal("425.00"),
                timestamp, "corr-123"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(refundAuditRecordService.save(any(RefundAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.refunded", 0, 7L);

        // Assert
        ArgumentCaptor<RefundAuditRecord> captor = ArgumentCaptor.forClass(RefundAuditRecord.class);
        verify(refundAuditRecordService).save(captor.capture());

        RefundAuditRecord saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(eventId, saved.eventId());
        assertEquals(refundId, saved.refundId());
        assertEquals(paymentId, saved.paymentId());
        assertEquals(walletId, saved.walletId());
        assertEquals(userId, saved.userId());
        assertEquals(new BigDecimal("75.00"), saved.amount());
        assertEquals("USD", saved.currency());
        assertEquals("Defective product", saved.reason());
        assertEquals("REF-001", saved.reference());
        assertEquals("corr-123", saved.correlationId());
        assertEquals(timestamp, saved.eventTimestamp());
        assertNotNull(saved.ingestedAt());
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.refunded"),
                eq(0), eq(7L), any());
    }

    @Test
    @DisplayName("Should handle event with null reason")
    void shouldHandleNullReason() {
        // Arrange
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                UUID.randomUUID(), "PAYMENT_REFUNDED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "EUR", null,
                "REF-002", new BigDecimal("200.00"),
                Instant.now(), null
        );

        when(processedEventRepository.insertIfAbsent(any(), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(refundAuditRecordService.save(any(RefundAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.refunded", 0, 8L);

        // Assert
        ArgumentCaptor<RefundAuditRecord> captor = ArgumentCaptor.forClass(RefundAuditRecord.class);
        verify(refundAuditRecordService).save(captor.capture());
        assertNull(captor.getValue().reason());
    }

    @Test
    @DisplayName("Should skip already-processed events without persisting a duplicate")
    void shouldSkipDuplicateEvents() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        PaymentRefundedEvent event = new PaymentRefundedEvent(
                eventId, "PAYMENT_REFUNDED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), "EUR", "Duplicate",
                "REF-DUP", new BigDecimal("100.00"),
                Instant.now(), "corr-dup"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(0);

        // Act
        consumer.consume(event, "payment.refunded", 2, 33L);

        // Assert - duplicate must not be persisted
        verify(refundAuditRecordService, never()).save(any(RefundAuditRecord.class));
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.refunded"),
                eq(2), eq(33L), any());
    }
}
