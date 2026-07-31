package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.model.RuleEvaluation;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudAssessmentRepositoryAdapter - Persistence Adapter")
class FraudAssessmentRepositoryAdapterTest {

    @Mock
    private FraudAssessmentJpaRepository jpaRepository;

    private FraudAssessmentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FraudAssessmentRepositoryAdapter(jpaRepository);
    }

    @Nested
    @DisplayName("When saving a fraud assessment")
    class WhenSavingFraudAssessment {

        @Test
        @DisplayName("Should convert domain model to JPA entity and save")
        void shouldConvertAndSave() {
            // Arrange
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds"),
                    new RuleEvaluation("VELOCITY_CHECK", 0, false, "normal"));

            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 30,
                    FraudDecision.REVIEW, evaluations, Instant.now());

            when(jpaRepository.save(any(FraudAssessmentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            FraudAssessment saved = adapter.save(assessment);

            // Assert
            assertNotNull(saved);
            assertEquals(assessmentId, saved.getAssessmentId());
            assertEquals(FraudDecision.REVIEW, saved.getDecision());
            assertEquals(30, saved.getRiskScore());

            ArgumentCaptor<FraudAssessmentJpaEntity> captor = ArgumentCaptor.forClass(FraudAssessmentJpaEntity.class);
            verify(jpaRepository).save(captor.capture());

            FraudAssessmentJpaEntity entity = captor.getValue();
            assertEquals(assessmentId, entity.getId());
            assertEquals(transactionId, entity.getTransactionId());
            assertEquals("TRANSFER", entity.getTransactionType());
            assertEquals(30, entity.getRiskScore());
            assertEquals(FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW, entity.getDecision());
        }

        @Test
        @DisplayName("Should serialize rule evaluations to JSON")
        void shouldSerializeRuleEvaluationsToJson() {
            // Arrange
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds"));
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 30,
                    FraudDecision.REVIEW, evaluations, Instant.now());

            when(jpaRepository.save(any(FraudAssessmentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            adapter.save(assessment);

            // Assert
            ArgumentCaptor<FraudAssessmentJpaEntity> captor = ArgumentCaptor.forClass(FraudAssessmentJpaEntity.class);
            verify(jpaRepository).save(captor.capture());

            String json = captor.getValue().getRulesEvaluatedJson();
            assertNotNull(json);
            assertTrue(json.contains("AMOUNT_THRESHOLD"));
            assertTrue(json.contains("30"));
        }
    }

    @Nested
    @DisplayName("When finding by ID")
    class WhenFindingById {

        @Test
        @DisplayName("Should return assessment when found")
        void shouldReturnAssessmentWhenFound() {
            // Arrange
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();

            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    assessmentId, transactionId, "TRANSFER", 50,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW,
                    List.of(new RuleEvaluation("AMOUNT", 50, true, "exceeds")),
                    timestamp);

            when(jpaRepository.findById(assessmentId)).thenReturn(Optional.of(entity));

            // Act
            Optional<FraudAssessment> result = adapter.findById(assessmentId);

            // Assert
            assertTrue(result.isPresent());
            FraudAssessment found = result.get();
            assertEquals(assessmentId, found.getAssessmentId());
            assertEquals(transactionId, found.getTransactionId());
            assertEquals("TRANSFER", found.getTransactionType());
            assertEquals(50, found.getRiskScore());
            assertEquals(FraudDecision.REVIEW, found.getDecision());
            assertEquals(timestamp, found.getTimestamp());
            assertEquals(1, found.getRulesEvaluated().size());
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Arrange
            UUID assessmentId = UUID.randomUUID();
            when(jpaRepository.findById(assessmentId)).thenReturn(Optional.empty());

            // Act
            Optional<FraudAssessment> result = adapter.findById(assessmentId);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should deserialize rule evaluations from JSON")
        void shouldDeserializeRuleEvaluationsFromJson() {
            // Arrange
            UUID assessmentId = UUID.randomUUID();
            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    assessmentId, UUID.randomUUID(), "TRANSFER", 30,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.APPROVE,
                    List.of(new RuleEvaluation("VELOCITY", 0, false, "normal")),
                    Instant.now());

            when(jpaRepository.findById(assessmentId)).thenReturn(Optional.of(entity));

            // Act
            Optional<FraudAssessment> result = adapter.findById(assessmentId);

            // Assert
            assertTrue(result.isPresent());
            List<RuleEvaluation> evaluations = result.get().getRulesEvaluated();
            assertEquals(1, evaluations.size());
            assertEquals("VELOCITY", evaluations.get(0).ruleName());
            assertEquals(0, evaluations.get(0).score());
            assertFalse(evaluations.get(0).matched());
        }
    }
}
