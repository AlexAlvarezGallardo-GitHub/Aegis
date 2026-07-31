package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.springframework.stereotype.Component;

@Component
public class VelocityRuleEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRule.RuleType supportedType() {
        return FraudRule.RuleType.VELOCITY;
    }

    @Override
    public RuleEvaluation evaluate(FraudRule rule, TransactionContext context) {
        boolean matched = context.recentTransactionsCount() >= rule.threshold();
        String details = matched
                ? context.recentTransactionsCount() + " transactions in recent window (threshold " + rule.threshold() + ")"
                : "Velocity normal (" + context.recentTransactionsCount() + " < " + rule.threshold() + ")";
        int score = matched ? rule.weight() : 0;
        return new RuleEvaluation(rule.name(), score, matched, details);
    }
}
