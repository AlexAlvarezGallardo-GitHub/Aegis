package com.aegis.fraud.infrastructure.messaging;

import com.aegis.fraud.domain.event.FraudAssessmentCompleted;
import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.model.RuleEvaluation;
import com.aegis.fraud.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.fraud.infrastructure.persistence.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher - Outbox Persistence")
class KafkaEventPublisherTest {

    @Mock
    private OutboxEventJpaRepository outboxRepository;

    private ObjectMapper objectMapper;
    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        publisher = new KafkaEventPublisher(outboxRepository, objectMapper);
    }

    @Nested
    @DisplayName("When publishing FraudAssessmentCompleted event")
    class WhenPublishingFraudAssessmentCompletedEvent {

        @Test
        @DisplayName("Should serialize event to JSON and persist to outbox table")
        void shouldSerializeEventAndPersistToOutbox() {
            // Arrange
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 50,
                    FraudDecision.REVIEW,
                    List.of(new RuleEvaluation("AMOUNT", 30, true, "exceeds")),
                    Instant.now());
            FraudAssessmentCompleted event = new FraudAssessmentCompleted(assessment);

            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            publisher.publish(event);

            // Assert
            ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository).save(captor.capture());

            OutboxEventJpaEntity saved = captor.getValue();
            assertNotNull(saved.getId());
            assertEquals("FRAUD_ASSESSMENT", saved.getAggregateType());
            assertEquals(assessmentId, saved.getAggregateId());
            assertEquals("FRAUD_ASSESSMENT_COMPLETED", saved.getEventType());
            assertEquals("PENDING", saved.getStatus());
            assertNotNull(saved.getCreatedAt());
            assertNull(saved.getPublishedAt());
        }

        @Test
        @DisplayName("Should produce valid JSON payload containing all event fields")
        void shouldProduceValidJsonPayload() throws Exception {
            // Arrange
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 50,
                    FraudDecision.REVIEW,
                    List.of(new RuleEvaluation("AMOUNT", 30, true, "exceeds")),
                    Instant.now());
            FraudAssessmentCompleted event = new FraudAssessmentCompleted(assessment);

            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            publisher.publish(event);

            // Assert
            ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository).save(captor.capture());

            String payload = captor.getValue().getPayload();
            assertNotNull(payload);

            var payloadMap = objectMapper.readValue(payload, java.util.Map.class);
            assertEquals("FRAUD_ASSESSMENT_COMPLETED", payloadMap.get("eventType"));
            assertEquals("1.0", payloadMap.get("schemaVersion"));
            assertEquals(assessmentId.toString(), payloadMap.get("assessmentId"));
            assertEquals(transactionId.toString(), payloadMap.get("transactionId"));
            assertEquals("TRANSFER", payloadMap.get("transactionType"));
            assertEquals(50, payloadMap.get("riskScore"));
            assertEquals("REVIEW", payloadMap.get("decision"));
        }

        @Test
        @DisplayName("Should set initial status to PENDING")
        void shouldSetInitialStatusToPending() {
            // Arrange
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());
            FraudAssessmentCompleted event = new FraudAssessmentCompleted(assessment);

            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            publisher.publish(event);

            // Assert
            ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository).save(captor.capture());
            assertEquals("PENDING", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should generate unique outbox event ID for each publish")
        void shouldGenerateUniqueOutboxEventId() {
            // Arrange
            when(outboxRepository.save(any(OutboxEventJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            FraudAssessment assessment1 = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());
            FraudAssessment assessment2 = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());

            FraudAssessmentCompleted event1 = new FraudAssessmentCompleted(assessment1);
            FraudAssessmentCompleted event2 = new FraudAssessmentCompleted(assessment2);

            // Act
            publisher.publish(event1);
            publisher.publish(event2);

            // Assert
            var captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
            verify(outboxRepository, times(2)).save(captor.capture());

            UUID id1 = captor.getAllValues().get(0).getId();
            UUID id2 = captor.getAllValues().get(1).getId();
            assertNotEquals(id1, id2, "Each outbox event must have a unique ID");
        }
    }

    @Nested
    @DisplayName("When database persistence fails")
    class WhenDatabasePersistenceFails {

        @Test
        @DisplayName("Should propagate exception from outbox repository save")
        void shouldPropagateExceptionFromRepository() {
            // Arrange
            when(outboxRepository.save(any(OutboxEventJpaEntity.class)))
                    .thenThrow(new RuntimeException("Database connection lost"));

            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());
            FraudAssessmentCompleted event = new FraudAssessmentCompleted(assessment);

            // Act & Assert
            assertThrows(RuntimeException.class, () -> publisher.publish(event));
        }
    }
}
