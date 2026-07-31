package com.aegis.fraud.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudAssessment - Domain Model")
class FraudAssessmentTest {

    @Nested
    @DisplayName("When creating a complete assessment")
    class WhenCreatingCompleteAssessment {

        @Test
        @DisplayName("Should generate UUID v7 assessment ID")
        void shouldGenerateUuidV7AssessmentId() {
            FraudAssessment assessment = FraudAssessment.complete(
                    UUID.randomUUID(), "TRANSFER", 50,
                    FraudDecision.REVIEW, List.of());

            assertNotNull(assessment.getAssessmentId());
            // UUID v7 has version 7 in the most significant bits
            assertEquals(7, assessment.getAssessmentId().version());
        }

        @Test
        @DisplayName("Should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            UUID transactionId = UUID.randomUUID();
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT", 30, true, "exceeds"));

            FraudAssessment assessment = FraudAssessment.complete(
                    transactionId, "TRANSFER", 30,
                    FraudDecision.REVIEW, evaluations);

            assertEquals(transactionId, assessment.getTransactionId());
            assertEquals("TRANSFER", assessment.getTransactionType());
            assertEquals(30, assessment.getRiskScore());
            assertEquals(FraudDecision.REVIEW, assessment.getDecision());
            assertEquals(evaluations, assessment.getRulesEvaluated());
            assertNotNull(assessment.getTimestamp());
        }

        @Test
        @DisplayName("Should create immutable copy of evaluations list")
        void shouldCreateImmutableCopyOfEvaluationsList() {
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT", 30, true, "exceeds"));

            FraudAssessment assessment = FraudAssessment.complete(
                    UUID.randomUUID(), "TRANSFER", 30,
                    FraudDecision.REVIEW, evaluations);

            assertThrows(UnsupportedOperationException.class,
                    () -> assessment.getRulesEvaluated().add(
                            new RuleEvaluation("VELOCITY", 25, true, "high")));
        }
    }

    @Nested
    @DisplayName("When rehydrating an assessment")
    class WhenRehydratingAssessment {

        @Test
        @DisplayName("Should preserve the original assessment ID")
        void shouldPreserveOriginalAssessmentId() {
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 50,
                    FraudDecision.REVIEW, List.of(), timestamp);

            assertEquals(assessmentId, assessment.getAssessmentId());
            assertEquals(transactionId, assessment.getTransactionId());
            assertEquals(timestamp, assessment.getTimestamp());
        }

        @Test
        @DisplayName("Should handle empty evaluations list")
        void shouldHandleEmptyEvaluationsList() {
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());

            assertTrue(assessment.getRulesEvaluated().isEmpty());
        }
    }

    @Nested
    @DisplayName("FraudDecision enum")
    class FraudDecisionEnum {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            FraudDecision[] values = FraudDecision.values();
            assertEquals(3, values.length);
            assertEquals(FraudDecision.APPROVE, FraudDecision.valueOf("APPROVE"));
            assertEquals(FraudDecision.REVIEW, FraudDecision.valueOf("REVIEW"));
            assertEquals(FraudDecision.REJECT, FraudDecision.valueOf("REJECT"));
        }
    }

    @Nested
    @DisplayName("RuleEvaluation record")
    class RuleEvaluationRecord {

        @Test
        @DisplayName("Should store all fields correctly")
        void shouldStoreAllFieldsCorrectly() {
            RuleEvaluation evaluation = new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds");

            assertEquals("AMOUNT_THRESHOLD", evaluation.ruleName());
            assertEquals(30, evaluation.score());
            assertTrue(evaluation.matched());
            assertEquals("exceeds", evaluation.details());
        }
    }

    @Nested
    @DisplayName("FraudRule record")
    class FraudRuleRecord {

        @Test
        @DisplayName("Should create with generated UUID v7")
        void shouldCreateWithGeneratedUuidV7() {
            FraudRule rule = FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 30);

            assertNotNull(rule.id());
            assertEquals(7, rule.id().version());
            assertEquals("AMOUNT_THRESHOLD", rule.name());
            assertEquals(FraudRule.RuleType.AMOUNT, rule.type());
            assertEquals(1000, rule.threshold());
            assertEquals(30, rule.weight());
            assertTrue(rule.enabled());
        }

        @Test
        @DisplayName("RuleType enum should have all expected values")
        void ruleTypeEnumShouldHaveAllExpectedValues() {
            FraudRule.RuleType[] values = FraudRule.RuleType.values();
            assertEquals(4, values.length);
            assertEquals(FraudRule.RuleType.VELOCITY, FraudRule.RuleType.valueOf("VELOCITY"));
            assertEquals(FraudRule.RuleType.AMOUNT, FraudRule.RuleType.valueOf("AMOUNT"));
            assertEquals(FraudRule.RuleType.GEOGRAPHIC, FraudRule.RuleType.valueOf("GEOGRAPHIC"));
            assertEquals(FraudRule.RuleType.TIME, FraudRule.RuleType.valueOf("TIME"));
        }
    }
}
