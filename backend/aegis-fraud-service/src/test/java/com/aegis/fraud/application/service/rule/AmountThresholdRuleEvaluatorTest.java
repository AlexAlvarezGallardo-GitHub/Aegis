package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AmountThresholdRuleEvaluatorTest {

    private final AmountThresholdRuleEvaluator evaluator = new AmountThresholdRuleEvaluator();

    private TransactionContext context(BigDecimal amount) {
        return new TransactionContext(
                UUID.randomUUID(), "TRANSFER", amount, "EUR",
                null, null, UUID.randomUUID(),
                null, null, 0, Instant.now());
    }

    @Test
    void shouldMatchWhenAmountExceedsThreshold() {
        FraudRule rule = FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 30);
        RuleEvaluation result = evaluator.evaluate(rule, context(new BigDecimal("1500.00")));

        assertTrue(result.matched());
        assertEquals(30, result.score());
    }

    @Test
    void shouldNotMatchWhenAmountWithinThreshold() {
        FraudRule rule = FraudRule.create("AMOUNT_THRESHOLD", FraudRule.RuleType.AMOUNT, 1000, 30);
        RuleEvaluation result = evaluator.evaluate(rule, context(new BigDecimal("500.00")));

        assertFalse(result.matched());
        assertEquals(0, result.score());
    }
}
