package com.aegis.fraud.application.service;

import com.aegis.fraud.application.service.rule.FraudRuleEvaluator;
import com.aegis.fraud.application.service.rule.TransactionContext;
import com.aegis.fraud.domain.exception.AssessmentNotFoundException;
import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.domain.port.outbound.EventPublisher;
import com.aegis.fraud.domain.port.outbound.FraudAssessmentRepository;
import com.aegis.fraud.domain.port.outbound.FraudRuleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service implementing the fraud assessment use case.
 */
@Service
public class AssessFraudService implements AssessFraudUseCase {

    private static final int DEFAULT_RECENT_TRANSACTION_COUNT = 0;

    private final FraudRuleRepository ruleRepository;
    private final FraudAssessmentRepository assessmentRepository;
    private final EventPublisher eventPublisher;
    private final RiskScorer riskScorer;
    private final DecisionMaker decisionMaker;
    private final Map<FraudRule.RuleType, FraudRuleEvaluator> evaluators;
    private final String expectedCountryCode;

    public AssessFraudService(FraudRuleRepository ruleRepository,
                              FraudAssessmentRepository assessmentRepository,
                              EventPublisher eventPublisher,
                              RiskScorer riskScorer,
                              DecisionMaker decisionMaker,
                              List<FraudRuleEvaluator> ruleEvaluators,
                              @Value("${aegis.fraud.expected-country:ES}") String expectedCountryCode) {
        this.ruleRepository = ruleRepository;
        this.assessmentRepository = assessmentRepository;
        this.eventPublisher = eventPublisher;
        this.riskScorer = riskScorer;
        this.decisionMaker = decisionMaker;
        this.evaluators = new EnumMap<>(FraudRule.RuleType.class);
        for (FraudRuleEvaluator evaluator : ruleEvaluators) {
            this.evaluators.put(evaluator.supportedType(), evaluator);
        }
        this.expectedCountryCode = expectedCountryCode;
    }

    @Override
    @Transactional
    public FraudAssessment assess(AssessmentCommand command) {
        List<FraudRule> rules = ruleRepository.findEnabledRules();
        TransactionContext context = buildContext(command);

        List<RuleEvaluation> evaluations = rules.stream()
                .filter(rule -> evaluators.containsKey(rule.type()))
                .map(rule -> evaluators.get(rule.type()).evaluate(rule, context))
                .toList();

        int riskScore = riskScorer.score(evaluations);
        var decision = decisionMaker.decide(riskScore);

        FraudAssessment assessment = FraudAssessment.complete(
                command.transactionId(), command.transactionType(),
                riskScore, decision, evaluations);

        FraudAssessment saved = assessmentRepository.save(assessment);
        eventPublisher.publish(new com.aegis.fraud.domain.event.FraudAssessmentCompleted(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public FraudAssessment findById(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }

    private TransactionContext buildContext(AssessmentCommand command) {
        return new TransactionContext(
                command.transactionId(),
                command.transactionType(),
                command.amount(),
                command.currency(),
                command.sourceWalletId(),
                command.destWalletId(),
                command.userId(),
                command.countryCode(),
                expectedCountryCode,
                DEFAULT_RECENT_TRANSACTION_COUNT,
                Instant.now()
        );
    }
}
