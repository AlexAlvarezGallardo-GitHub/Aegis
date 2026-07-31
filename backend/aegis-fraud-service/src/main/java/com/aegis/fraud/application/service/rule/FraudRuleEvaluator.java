package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;

/**
 * Strategy interface for evaluating a fraud rule against a transaction context.
 */
public interface FraudRuleEvaluator {

    FraudRule.RuleType supportedType();

    RuleEvaluation evaluate(FraudRule rule, TransactionContext context);
}
