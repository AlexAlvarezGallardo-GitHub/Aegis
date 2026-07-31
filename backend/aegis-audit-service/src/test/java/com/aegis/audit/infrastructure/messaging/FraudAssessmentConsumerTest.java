package com.aegis.audit.infrastructure.messaging;

import com.aegis.audit.application.service.FraudAuditRecordService;
import com.aegis.audit.domain.event.FraudAssessmentCompletedEvent;
import com.aegis.audit.domain.model.FraudAuditRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudAssessmentConsumer - Kafka Consumer")
class FraudAssessmentConsumerTest {

    @Mock
    private FraudAuditRecordService fraudAuditRecordService;

    private ObjectMapper objectMapper;
    private FraudAssessmentConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new FraudAssessmentConsumer(fraudAuditRecordService, objectMapper);
    }

    @Test
    @DisplayName("Should map event to FraudAuditRecord and persist via service")
    void shouldMapAndPersistFraudAuditRecord() {
        // Arrange
        UUID assessmentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        FraudAssessmentCompletedEvent event = new FraudAssessmentCompletedEvent(
                UUID.randomUUID(), "FRAUD_ASSESSMENT_COMPLETED", "1.0",
                assessmentId, transactionId, "TRANSFER", 75, "REVIEW",
                List.of(new FraudAssessmentCompletedEvent.RuleEvaluation("rule1", 75, true, "suspicious")),
                timestamp
        );

        when(fraudAuditRecordService.save(any(FraudAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event);

        // Assert
        ArgumentCaptor<FraudAuditRecord> captor = ArgumentCaptor.forClass(FraudAuditRecord.class);
        verify(fraudAuditRecordService).save(captor.capture());

        FraudAuditRecord saved = captor.getValue();
        assertNotNull(saved.id());
        assertEquals(assessmentId, saved.assessmentId());
        assertEquals(transactionId, saved.transactionId());
        assertEquals("TRANSFER", saved.transactionType());
        assertEquals(75, saved.riskScore());
        assertEquals("REVIEW", saved.decision());
        assertNotNull(saved.rulesEvaluated());
        assertTrue(saved.rulesEvaluated().contains("rule1"));
        assertEquals(timestamp, saved.eventTimestamp());
        assertNotNull(saved.ingestedAt());
    }

    @Test
    @DisplayName("Should handle event with empty rules list")
    void shouldHandleEmptyRulesList() {
        // Arrange
        FraudAssessmentCompletedEvent event = new FraudAssessmentCompletedEvent(
                UUID.randomUUID(), "FRAUD_ASSESSMENT_COMPLETED", "1.0",
                UUID.randomUUID(), UUID.randomUUID(), "WITHDRAWAL", 10, "APPROVED",
                List.of(), Instant.now()
        );

        when(fraudAuditRecordService.save(any(FraudAuditRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        consumer.consume(event);

        // Assert
        verify(fraudAuditRecordService).save(any(FraudAuditRecord.class));
    }
}
