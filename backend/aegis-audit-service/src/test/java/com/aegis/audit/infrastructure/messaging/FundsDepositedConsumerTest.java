package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.AuditRecordService;
import com.aegis.audit.domain.event.FundsDepositedEvent;
import com.aegis.audit.domain.model.AuditRecord;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FundsDepositedConsumer - Kafka Consumer")
class FundsDepositedConsumerTest {

    @Mock
    private AuditRecordService auditRecordService;

    private FundsDepositedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new FundsDepositedConsumer(auditRecordService);
    }

    @Test
    @DisplayName("Should map event to AuditRecord and persist via service")
    void shouldMapAndPersistAuditRecord() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        FundsDepositedEvent event = new FundsDepositedEvent(
                UUID.randomUUID(), "FUNDS_DEPOSITED", "1.0",
                walletId, userId, new BigDecimal("100.00"), "USD",
                "BANK_TRANSFER", "REF-001", new BigDecimal("500.00"),
                timestamp, "corr-123"
        );

        when(auditRecordService.save(any(AuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event);

        // Assert
        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordService).save(captor.capture());

        AuditRecord saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(walletId, saved.walletId());
        assertEquals(userId, saved.userId());
        assertEquals(new BigDecimal("100.00"), saved.amount());
        assertEquals("USD", saved.currency());
        assertEquals("BANK_TRANSFER", saved.source());
        assertEquals("REF-001", saved.reference());
        assertEquals(new BigDecimal("500.00"), saved.newBalance());
        assertEquals(timestamp, saved.eventTimestamp());
        assertNotNull(saved.ingestedAt());
        assertEquals("corr-123", saved.correlationId());
    }

    @Test
    @DisplayName("Should handle event with null optional fields")
    void shouldHandleNullOptionalFields() {
        // Arrange
        FundsDepositedEvent event = new FundsDepositedEvent(
                UUID.randomUUID(), "FUNDS_DEPOSITED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), "EUR",
                null, null, new BigDecimal("200.00"), Instant.now(), null
        );

        when(auditRecordService.save(any(AuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event);

        // Assert
        verify(auditRecordService).save(any(AuditRecord.class));
    }
}
