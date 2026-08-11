package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.TransferAuditRecord;
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
@DisplayName("TransferAuditRecordRepositoryAdapter - Persistence Adapter")
class TransferAuditRecordRepositoryAdapterTest {

    @Mock
    private TransferAuditRecordJpaRepository jpaRepository;

    private TransferAuditRecordRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TransferAuditRecordRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        TransferAuditRecord record = new TransferAuditRecord(
                id, eventId, transferId, "REQUESTED",
                sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "REF-001",
                null, "corr-123", now, now
        );

        when(jpaRepository.save(any(TransferAuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TransferAuditRecord saved = adapter.save(record);

        // Assert
        ArgumentCaptor<TransferAuditRecordJpaEntity> captor = ArgumentCaptor.forClass(TransferAuditRecordJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        TransferAuditRecordJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(eventId, entity.getEventId());
        assertEquals(transferId, entity.getTransferId());
        assertEquals("REQUESTED", entity.getEventType());
        assertEquals(sourceWalletId, entity.getSourceWalletId());
        assertEquals(destWalletId, entity.getDestWalletId());
        assertEquals(userId, entity.getUserId());
        assertEquals(new BigDecimal("100.00"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals("REF-001", entity.getReference());
        assertNull(entity.getFailureReason());
        assertEquals("corr-123", entity.getCorrelationId());

        assertNotNull(saved);
        assertEquals(id, saved.id());
    }
}
