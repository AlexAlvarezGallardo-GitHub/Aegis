package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.springframework.stereotype.Component;

@Component
public class GeographicRuleEvaluator implements FraudRuleEvaluator {

    @Override
    public FraudRule.RuleType supportedType() {
        return FraudRule.RuleType.GEOGRAPHIC;
    }

    @Override
    public RuleEvaluation evaluate(FraudRule rule, TransactionContext context) {
        boolean hasCountry = context.countryCode() != null && context.expectedCountryCode() != null;
        boolean matched = hasCountry && !context.countryCode().equalsIgnoreCase(context.expectedCountryCode());
        String details = !hasCountry
                ? "No country data for geographic check"
                : matched
                ? "Transaction country " + context.countryCode() + " differs from expected " + context.expectedCountryCode()
                : "Geographic pattern normal";
        int score = matched ? rule.weight() : 0;
        return new RuleEvaluation(rule.name(), score, matched, details);
    }
}
