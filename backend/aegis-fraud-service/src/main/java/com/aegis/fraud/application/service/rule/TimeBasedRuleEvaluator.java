package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class TimeBasedRuleEvaluator implements FraudRuleEvaluator {

    private static final int OFF_HOURS_START = 23;
    private static final int OFF_HOURS_END = 6;

    @Override
    public FraudRule.RuleType supportedType() {
        return FraudRule.RuleType.TIME;
    }

    @Override
    public RuleEvaluation evaluate(FraudRule rule, TransactionContext context) {
        int hour = context.timestamp().atOffset(ZoneOffset.UTC).getHour();
        boolean offHours = hour >= OFF_HOURS_START || hour < OFF_HOURS_END;
        boolean matched = offHours;
        String details = matched
                ? "Transaction at " + context.timestamp() + " outside normal hours (UTC " + OFF_HOURS_START + ":00-" + OFF_HOURS_END + ":00)"
                : "Transaction within normal hours";
        int score = matched ? rule.weight() : 0;
        return new RuleEvaluation(rule.name(), score, matched, details);
    }
}
