package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudAssessmentJpaEntity - Persistence Entity")
class FraudAssessmentJpaEntityTest {

    @Nested
    @DisplayName("When creating a new assessment entity")
    class WhenCreatingNewAssessmentEntity {

        @Test
        @DisplayName("Should initialize all fields correctly")
        void shouldInitializeAllFieldsCorrectly() {
            UUID id = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds"),
                    new RuleEvaluation("VELOCITY_CHECK", 0, false, "normal"));

            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    id, transactionId, "TRANSFER", 30,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW,
                    evaluations, timestamp);

            assertEquals(id, entity.getId());
            assertEquals(transactionId, entity.getTransactionId());
            assertEquals("TRANSFER", entity.getTransactionType());
            assertEquals(30, entity.getRiskScore());
            assertEquals(FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW, entity.getDecision());
            assertEquals(timestamp, entity.getTimestamp());
            assertNotNull(entity.getRulesEvaluatedJson());
        }

        @Test
        @DisplayName("Should serialize rule evaluations to JSON")
        void shouldSerializeRuleEvaluationsToJson() {
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds"));

            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 30,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW,
                    evaluations, Instant.now());

            String json = entity.getRulesEvaluatedJson();
            assertNotNull(json);
            assertTrue(json.contains("AMOUNT_THRESHOLD"));
            assertTrue(json.contains("30"));
            assertTrue(json.contains("true"));
        }

        @Test
        @DisplayName("Should handle empty evaluations list")
        void shouldHandleEmptyEvaluationsList() {
            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.APPROVE,
                    List.of(), Instant.now());

            String json = entity.getRulesEvaluatedJson();
            assertNotNull(json);
            assertEquals("[]", json);
        }
    }

    @Nested
    @DisplayName("When deserializing rule evaluations")
    class WhenDeserializingRuleEvaluations {

        @Test
        @DisplayName("Should deserialize JSON back to RuleEvaluation list")
        void shouldDeserializeJsonBackToRuleEvaluationList() {
            List<RuleEvaluation> originalEvaluations = List.of(
                    new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds threshold"),
                    new RuleEvaluation("VELOCITY_CHECK", 25, true, "high velocity"));

            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 55,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW,
                    originalEvaluations, Instant.now());

            List<RuleEvaluation> deserialized = entity.toRuleEvaluations();

            assertEquals(2, deserialized.size());
            assertEquals("AMOUNT_THRESHOLD", deserialized.get(0).ruleName());
            assertEquals(30, deserialized.get(0).score());
            assertTrue(deserialized.get(0).matched());
            assertEquals("exceeds threshold", deserialized.get(0).details());

            assertEquals("VELOCITY_CHECK", deserialized.get(1).ruleName());
            assertEquals(25, deserialized.get(1).score());
            assertTrue(deserialized.get(1).matched());
        }

        @Test
        @DisplayName("Should return empty list for empty JSON array")
        void shouldReturnEmptyListForEmptyJsonArray() {
            FraudAssessmentJpaEntity entity = new FraudAssessmentJpaEntity(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.APPROVE,
                    List.of(), Instant.now());

            List<RuleEvaluation> deserialized = entity.toRuleEvaluations();

            assertTrue(deserialized.isEmpty());
        }
    }

    @Nested
    @DisplayName("FraudDecisionJpa enum")
    class FraudDecisionJpaEnum {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            FraudAssessmentJpaEntity.FraudDecisionJpa[] values = FraudAssessmentJpaEntity.FraudDecisionJpa.values();
            assertEquals(3, values.length);
            assertEquals(FraudAssessmentJpaEntity.FraudDecisionJpa.APPROVE,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.valueOf("APPROVE"));
            assertEquals(FraudAssessmentJpaEntity.FraudDecisionJpa.REVIEW,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.valueOf("REVIEW"));
            assertEquals(FraudAssessmentJpaEntity.FraudDecisionJpa.REJECT,
                    FraudAssessmentJpaEntity.FraudDecisionJpa.valueOf("REJECT"));
        }
    }

    @Nested
    @DisplayName("Static helper methods")
    class StaticHelperMethods {

        @Test
        @DisplayName("emptyEvaluations should return empty list")
        void emptyEvaluationsShouldReturnEmptyList() {
            List<RuleEvaluation> empty = FraudAssessmentJpaEntity.emptyEvaluations();
            assertNotNull(empty);
            assertTrue(empty.isEmpty());
        }
    }
}
