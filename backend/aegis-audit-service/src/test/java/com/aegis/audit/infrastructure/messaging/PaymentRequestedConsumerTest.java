package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.PaymentAuditRecordService;
import com.aegis.audit.domain.event.Payee;
import com.aegis.audit.domain.event.PaymentRequestedEvent;
import com.aegis.audit.domain.model.PaymentAuditRecord;
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
@DisplayName("PaymentRequestedConsumer - Kafka Consumer")
class PaymentRequestedConsumerTest {

    @Mock
    private PaymentAuditRecordService paymentAuditRecordService;

    @Mock
    private ProcessedEventJpaRepository processedEventRepository;

    private PaymentRequestedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PaymentRequestedConsumer(paymentAuditRecordService, processedEventRepository);
    }

    @Test
    @DisplayName("Should map event to PaymentAuditRecord and persist via service")
    void shouldMapAndPersistPaymentAuditRecord() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        Payee payee = new Payee("Acme Corp", "PAYEE-1", "MERCHANT");

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId, "PAYMENT_REQUESTED", "1.0",
                paymentId, walletId, userId,
                new BigDecimal("100.00"), "USD", payee, "REF-001",
                timestamp, "corr-123"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(paymentAuditRecordService.save(any(PaymentAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.requested", 0, 7L);

        // Assert
        ArgumentCaptor<PaymentAuditRecord> captor = ArgumentCaptor.forClass(PaymentAuditRecord.class);
        verify(paymentAuditRecordService).save(captor.capture());

        PaymentAuditRecord saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(eventId, saved.eventId());
        assertEquals(paymentId, saved.paymentId());
        assertEquals("REQUESTED", saved.eventType());
        assertEquals(walletId, saved.walletId());
        assertEquals(userId, saved.userId());
        assertEquals(new BigDecimal("100.00"), saved.amount());
        assertEquals("USD", saved.currency());
        assertEquals("Acme Corp", saved.payeeName());
        assertNull(saved.failureReason());
        assertEquals("corr-123", saved.correlationId());
        assertEquals(timestamp, saved.eventTimestamp());
        assertNotNull(saved.ingestedAt());
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.requested"),
                eq(0), eq(7L), any());
    }

    @Test
    @DisplayName("Should handle event with null payee")
    void shouldHandleNullPayee() {
        // Arrange
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                UUID.randomUUID(), "PAYMENT_REQUESTED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), "EUR", null, null,
                Instant.now(), null
        );

        when(processedEventRepository.insertIfAbsent(any(), any(), anyInt(), anyLong(), any()))
                .thenReturn(1);
        when(paymentAuditRecordService.save(any(PaymentAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event, "payment.requested", 0, 8L);

        // Assert
        ArgumentCaptor<PaymentAuditRecord> captor = ArgumentCaptor.forClass(PaymentAuditRecord.class);
        verify(paymentAuditRecordService).save(captor.capture());
        assertNull(captor.getValue().payeeName());
    }

    @Test
    @DisplayName("Should skip already-processed events without persisting a duplicate")
    void shouldSkipDuplicateEvents() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId, "PAYMENT_REQUESTED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("10.00"), "EUR", new Payee("Shop", "S1", "MERCHANT"), "REF-DUP",
                Instant.now(), "corr-dup"
        );

        when(processedEventRepository.insertIfAbsent(eq(eventId), any(), anyInt(), anyLong(), any()))
                .thenReturn(0);

        // Act
        consumer.consume(event, "payment.requested", 2, 33L);

        // Assert - duplicate must not be persisted
        verify(paymentAuditRecordService, never()).save(any(PaymentAuditRecord.class));
        verify(processedEventRepository).insertIfAbsent(eq(eventId), eq("payment.requested"),
                eq(2), eq(33L), any());
    }
}
