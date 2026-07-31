package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TimeBasedRuleEvaluatorTest {

    private final TimeBasedRuleEvaluator evaluator = new TimeBasedRuleEvaluator();

    private TransactionContext contextAtUtcHour(int hour) {
        Instant timestamp = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();
        return new TransactionContext(
                UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                null, null, UUID.randomUUID(),
                null, null, 0, timestamp);
    }

    @Test
    void shouldMatchDuringOffHours() {
        FraudRule rule = FraudRule.create("OFF_HOURS_TRANSACTION", FraudRule.RuleType.TIME, 0, 15);
        RuleEvaluation result = evaluator.evaluate(rule, contextAtUtcHour(2));

        assertTrue(result.matched());
        assertEquals(15, result.score());
    }

    @Test
    void shouldNotMatchDuringBusinessHours() {
        FraudRule rule = FraudRule.create("OFF_HOURS_TRANSACTION", FraudRule.RuleType.TIME, 0, 15);
        RuleEvaluation result = evaluator.evaluate(rule, contextAtUtcHour(14));

        assertFalse(result.matched());
    }
}
