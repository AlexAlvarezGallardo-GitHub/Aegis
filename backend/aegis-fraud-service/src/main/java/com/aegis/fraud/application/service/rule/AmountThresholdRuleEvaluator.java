package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountThresholdRuleEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRule.RuleType supportedType() {
        return FraudRule.RuleType.AMOUNT;
    }

    @Override
    public RuleEvaluation evaluate(FraudRule rule, TransactionContext context) {
        BigDecimal threshold = BigDecimal.valueOf(rule.threshold());
        boolean matched = context.amount().compareTo(threshold) > 0;
        String details = matched
                ? "Amount " + context.amount() + " " + context.currency() + " exceeds threshold " + threshold
                : "Amount within threshold";
        int score = matched ? rule.weight() : 0;
        return new RuleEvaluation(rule.name(), score, matched, details);
    }
}
