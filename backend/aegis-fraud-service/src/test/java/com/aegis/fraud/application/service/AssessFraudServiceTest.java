package com.aegis.fraud.application.service;

import com.aegis.fraud.application.service.rule.AmountThresholdRuleEvaluator;
import com.aegis.fraud.application.service.rule.FraudRuleEvaluator;
import com.aegis.fraud.application.service.rule.VelocityRuleEvaluator;
import com.aegis.fraud.domain.event.FraudAssessmentCompleted;
import com.aegis.fraud.domain.exception.AssessmentNotFoundException;
import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.domain.port.outbound.EventPublisher;
import com.aegis.fraud.domain.port.outbound.FraudAssessmentRepository;
import com.aegis.fraud.domain.port.outbound.FraudRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssessFraudServiceTest {

    @Test
    void assessShouldApproveLowRiskTransaction() {
        var service = serviceWithRules(List.of(
                FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 30),
                FraudRule.create("VELOCITY_CHECK", FraudRule.RuleType.VELOCITY, 5, 25)));

        var command = command(new BigDecimal("100.00"));

        FraudAssessment assessment = service.assess(command);

        assertEquals(FraudDecision.APPROVE, assessment.getDecision());
        assertEquals(0, assessment.getRiskScore());
        assertEquals(2, assessment.getRulesEvaluated().size());
    }

    @Test
    void assessShouldRejectHighRiskTransaction() {
        var service = serviceWithRules(List.of(
                FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 80)));

        var command = command(new BigDecimal("5000.00"));

        FraudAssessment assessment = service.assess(command);

        assertEquals(FraudDecision.REJECT, assessment.getDecision());
        assertEquals(80, assessment.getRiskScore());
    }

    @Test
    void assessShouldPublishCompletedEvent() {
        var publisher = new StubEventPublisher();
        var service = serviceWithRules(
                List.of(FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 30)),
                publisher);

        service.assess(command(new BigDecimal("100.00")));

        assertEquals(1, publisher.events.size());
        FraudAssessmentCompleted event = publisher.events.getFirst();
        assertEquals("FRAUD_ASSESSMENT_COMPLETED", event.eventType());
        assertNotNull(event.assessmentId());
    }

    @Test
    void findByIdShouldReturnStoredAssessment() {
        var repository = new StubAssessmentRepository();
        var service = serviceWithRepository(repository);

        var assessment = service.assess(command(new BigDecimal("100.00")));
        FraudAssessment found = service.findById(assessment.getAssessmentId());

        assertEquals(assessment.getAssessmentId(), found.getAssessmentId());
        assertEquals(assessment.getDecision(), found.getDecision());
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        var service = serviceWithRules(List.of());
        assertThrows(AssessmentNotFoundException.class,
                () -> service.findById(UUID.randomUUID()));
    }

    private AssessFraudUseCase.AssessmentCommand command(BigDecimal amount) {
        return new AssessFraudUseCase.AssessmentCommand(
                UUID.randomUUID(), "TRANSFER", amount, "EUR",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
    }

    private AssessFraudService serviceWithRules(List<FraudRule> rules) {
        return serviceWithRules(rules, new StubEventPublisher());
    }

    private AssessFraudService serviceWithRules(List<FraudRule> rules, EventPublisher publisher) {
        return new AssessFraudService(
                new StubRuleRepository(rules),
                new StubAssessmentRepository(),
                publisher,
                new RiskScorer(),
                new DecisionMaker(30, 70),
                List.of(new AmountThresholdRuleEvaluator(), new VelocityRuleEvaluator()));
    }

    private AssessFraudService serviceWithRepository(StubAssessmentRepository repository) {
        return new AssessFraudService(
                new StubRuleRepository(List.of(
                        FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 30))),
                repository,
                new StubEventPublisher(),
                new RiskScorer(),
                new DecisionMaker(30, 70),
                List.of(new AmountThresholdRuleEvaluator()));
    }

    private static class StubRuleRepository implements FraudRuleRepository {
        private final List<FraudRule> rules;

        StubRuleRepository(List<FraudRule> rules) {
            this.rules = rules;
        }

        @Override
        public List<FraudRule> findEnabledRules() {
            return rules;
        }
    }

    private static class StubAssessmentRepository implements FraudAssessmentRepository {
        private final List<FraudAssessment> assessments = new ArrayList<>();

        @Override
        public FraudAssessment save(FraudAssessment assessment) {
            assessments.add(assessment);
            return assessment;
        }

        @Override
        public Optional<FraudAssessment> findById(UUID assessmentId) {
            return assessments.stream()
                    .filter(a -> a.getAssessmentId().equals(assessmentId))
                    .findFirst();
        }
    }

    private static class StubEventPublisher implements EventPublisher {
        private final List<FraudAssessmentCompleted> events = new ArrayList<>();

        @Override
        public void publish(FraudAssessmentCompleted event) {
            events.add(event);
        }
    }
}
