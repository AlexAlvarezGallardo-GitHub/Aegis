package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VelocityRuleEvaluatorTest {

    private final VelocityRuleEvaluator evaluator = new VelocityRuleEvaluator();

    private TransactionContext context(int recentTransactions) {
        return new TransactionContext(
                UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                null, null, UUID.randomUUID(),
                null, null, recentTransactions, Instant.now());
    }

    @Test
    void shouldMatchWhenTransactionCountReachesThreshold() {
        FraudRule rule = FraudRule.create("VELOCITY_CHECK", FraudRule.RuleType.VELOCITY, 5, 25);
        RuleEvaluation result = evaluator.evaluate(rule, context(5));

        assertTrue(result.matched());
        assertEquals(25, result.score());
    }

    @Test
    void shouldNotMatchWhenBelowThreshold() {
        FraudRule rule = FraudRule.create("VELOCITY_CHECK", FraudRule.RuleType.VELOCITY, 5, 25);
        RuleEvaluation result = evaluator.evaluate(rule, context(2));

        assertFalse(result.matched());
        assertEquals(0, result.score());
    }
}
