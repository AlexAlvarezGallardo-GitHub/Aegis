package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.FraudAuditRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudAuditRecordRepositoryAdapter - Persistence Adapter")
class FraudAuditRecordRepositoryAdapterTest {

    @Mock
    private FraudAuditRecordJpaRepository jpaRepository;

    private FraudAuditRecordRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FraudAuditRecordRepositoryAdapter(jpaRepository);
    }

    @Test
    @DisplayName("Should convert domain model to JPA entity and save")
    void shouldConvertAndSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Instant now = Instant.now();

        FraudAuditRecord record = new FraudAuditRecord(id, assessmentId, transactionId,
                "TRANSFER", 75, "REVIEW", "[{\"rule\":\"test\"}]", now, now);

        when(jpaRepository.save(any(FraudAuditRecordJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        FraudAuditRecord saved = adapter.save(record);

        // Assert
        ArgumentCaptor<FraudAuditRecordJpaEntity> captor = ArgumentCaptor.forClass(FraudAuditRecordJpaEntity.class);
        verify(jpaRepository).save(captor.capture());

        FraudAuditRecordJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals(assessmentId, entity.getAssessmentId());
        assertEquals(transactionId, entity.getTransactionId());
        assertEquals("TRANSFER", entity.getTransactionType());
        assertEquals(75, entity.getRiskScore());
        assertEquals("REVIEW", entity.getDecision());
        assertEquals("[{\"rule\":\"test\"}]", entity.getRulesEvaluated());

        assertNotNull(saved);
        assertEquals(id, saved.id());
    }
}
