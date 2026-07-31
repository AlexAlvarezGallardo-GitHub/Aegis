package com.aegis.fraud.application.dto;

import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssessmentResponse - DTO Mapping")
class AssessmentResponseTest {

    @Nested
    @DisplayName("When creating from FraudAssessment")
    class WhenCreatingFromFraudAssessment {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFieldsCorrectly() {
            UUID assessmentId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT_THRESHOLD", 30, true, "exceeds"));

            FraudAssessment assessment = FraudAssessment.rehydrate(
                    assessmentId, transactionId, "TRANSFER", 30,
                    FraudDecision.REVIEW, evaluations, timestamp);

            AssessmentResponse response = AssessmentResponse.from(assessment);

            assertEquals(assessmentId, response.assessmentId());
            assertEquals(transactionId, response.transactionId());
            assertEquals("TRANSFER", response.transactionType());
            assertEquals(30, response.riskScore());
            assertEquals("REVIEW", response.decision());
            assertEquals(evaluations, response.rulesEvaluated());
            assertEquals(timestamp, response.timestamp());
        }

        @Test
        @DisplayName("Should handle APPROVE decision")
        void shouldHandleApproveDecision() {
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 0,
                    FraudDecision.APPROVE, List.of(), Instant.now());

            AssessmentResponse response = AssessmentResponse.from(assessment);

            assertEquals("APPROVE", response.decision());
            assertEquals(0, response.riskScore());
            assertTrue(response.rulesEvaluated().isEmpty());
        }

        @Test
        @DisplayName("Should handle REJECT decision")
        void shouldHandleRejectDecision() {
            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 100,
                    FraudDecision.REJECT, List.of(), Instant.now());

            AssessmentResponse response = AssessmentResponse.from(assessment);

            assertEquals("REJECT", response.decision());
            assertEquals(100, response.riskScore());
        }

        @Test
        @DisplayName("Should handle multiple rule evaluations")
        void shouldHandleMultipleRuleEvaluations() {
            List<RuleEvaluation> evaluations = List.of(
                    new RuleEvaluation("AMOUNT", 30, true, "exceeds"),
                    new RuleEvaluation("VELOCITY", 25, true, "high"),
                    new RuleEvaluation("GEOGRAPHIC", 0, false, "normal"));

            FraudAssessment assessment = FraudAssessment.rehydrate(
                    UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 55,
                    FraudDecision.REVIEW, evaluations, Instant.now());

            AssessmentResponse response = AssessmentResponse.from(assessment);

            assertEquals(3, response.rulesEvaluated().size());
            assertEquals("AMOUNT", response.rulesEvaluated().get(0).ruleName());
            assertEquals("VELOCITY", response.rulesEvaluated().get(1).ruleName());
            assertEquals("GEOGRAPHIC", response.rulesEvaluated().get(2).ruleName());
        }
    }
}
