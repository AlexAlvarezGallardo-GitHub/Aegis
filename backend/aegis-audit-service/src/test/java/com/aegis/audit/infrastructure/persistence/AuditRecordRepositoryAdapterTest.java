package com.aegis.audit.infrastructure.persistence;

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
@DisplayName("AuditRecordRepositoryAdapter - Persistence Adapter")
class AuditRecordRepositoryAdapterTest {

    @Mock
    private AuditRecordJpaRepository jpaRepository;

    private AuditRecordRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuditRecordRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        AuditRecord record = new AuditRecord(id, walletId, userId, new BigDecimal("100.00"),
                "USD", "BANK_TRANSFER", "REF-001", new BigDecimal("500.00"),
                now, now, "corr-123");

        when(jpaRepository.save(any(AuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuditRecord saved = adapter.save(record);

        // Assert
        ArgumentCaptor<AuditRecordJpaEntity> captor = ArgumentCaptor.forClass(AuditRecordJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        AuditRecordJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(walletId, entity.getWalletId());
        assertEquals(userId, entity.getUserId());
        assertEquals(new BigDecimal("100.00"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());

        assertNotNull(saved);
        assertEquals(id, saved.id());
        assertEquals(walletId, saved.walletId());
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // Arrange
        AuditRecord record = new AuditRecord(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, "EUR", null, null, BigDecimal.TEN,
                Instant.now(), Instant.now(), null);

        when(jpaRepository.save(any(AuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuditRecord saved = adapter.save(record);

        // Assert
        assertNotNull(saved);
        assertNull(saved.source());
        assertNull(saved.reference());
        assertNull(saved.correlationId());
        verify(jpaRepository).save(any(AuditRecordJpaEntity.class));
    }
}
