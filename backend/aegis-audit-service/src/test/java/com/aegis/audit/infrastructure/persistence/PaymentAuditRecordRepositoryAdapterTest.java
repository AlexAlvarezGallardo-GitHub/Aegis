package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.PaymentAuditRecord;
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
@DisplayName("PaymentAuditRecordRepositoryAdapter - Persistence Adapter")
class PaymentAuditRecordRepositoryAdapterTest {

    @Mock
    private PaymentAuditRecordJpaRepository jpaRepository;

    private PaymentAuditRecordRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentAuditRecordRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentAuditRecord record = new PaymentAuditRecord(
                id, eventId, paymentId, "REQUESTED",
                walletId, userId,
                new BigDecimal("100.00"), "USD", "Acme Corp",
                null, "corr-123", now, now
        );

        when(jpaRepository.save(any(PaymentAuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PaymentAuditRecord saved = adapter.save(record);

        // Assert
        ArgumentCaptor<PaymentAuditRecordJpaEntity> captor = ArgumentCaptor.forClass(PaymentAuditRecordJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        PaymentAuditRecordJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(eventId, entity.getEventId());
        assertEquals(paymentId, entity.getPaymentId());
        assertEquals("REQUESTED", entity.getEventType());
        assertEquals(walletId, entity.getWalletId());
        assertEquals(userId, entity.getUserId());
        assertEquals(new BigDecimal("100.00"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals("Acme Corp", entity.getPayeeName());
        assertNull(entity.getFailureReason());
        assertEquals("corr-123", entity.getCorrelationId());

        assertNotNull(saved);
        assertEquals(id, saved.id());
    }
}
