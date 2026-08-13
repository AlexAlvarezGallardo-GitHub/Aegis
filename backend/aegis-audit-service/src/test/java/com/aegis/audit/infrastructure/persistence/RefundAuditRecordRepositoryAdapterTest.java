package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.RefundAuditRecord;
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
@DisplayName("RefundAuditRecordRepositoryAdapter - Persistence Adapter")
class RefundAuditRecordRepositoryAdapterTest {

    @Mock
    private RefundAuditRecordJpaRepository jpaRepository;

    private RefundAuditRecordRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefundAuditRecordRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        RefundAuditRecord record = new RefundAuditRecord(
                id, eventId, refundId, paymentId,
                walletId, userId,
                new BigDecimal("50.00"), "USD", "Defective product",
                "REF-001", "corr-123", now, now
        );

        when(jpaRepository.save(any(RefundAuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        RefundAuditRecord saved = adapter.save(record);

        // Assert
        ArgumentCaptor<RefundAuditRecordJpaEntity> captor = ArgumentCaptor.forClass(RefundAuditRecordJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        RefundAuditRecordJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(eventId, entity.getEventId());
        assertEquals(refundId, entity.getRefundId());
        assertEquals(paymentId, entity.getPaymentId());
        assertEquals(walletId, entity.getWalletId());
        assertEquals(userId, entity.getUserId());
        assertEquals(new BigDecimal("50.00"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals("Defective product", entity.getReason());
        assertEquals("REF-001", entity.getReference());
        assertEquals("corr-123", entity.getCorrelationId());
        assertEquals(now, entity.getEventTimestamp());
        assertEquals(now, entity.getIngestedAt());

        assertNotNull(saved);
        assertEquals(id, saved.id());
    }

    @Test
    @DisplayName("Should convert domain model with null reason to JPA entity")
    void shouldConvertWithNullReason() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        RefundAuditRecord record = new RefundAuditRecord(
                id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("25.00"), "EUR", null,
                "REF-002", null, now, now
        );

        when(jpaRepository.save(any(RefundAuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        RefundAuditRecord saved = adapter.save(record);

        // Assert
        ArgumentCaptor<RefundAuditRecordJpaEntity> captor = ArgumentCaptor.forClass(RefundAuditRecordJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        assertNull(captor.getValue().getReason());
        assertNull(captor.getValue().getCorrelationId());
        assertNotNull(saved);
    }
}
