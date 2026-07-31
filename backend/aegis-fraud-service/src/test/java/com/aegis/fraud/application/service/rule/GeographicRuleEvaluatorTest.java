package com.aegis.fraud.application.service.rule;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GeographicRuleEvaluatorTest {

    private final GeographicRuleEvaluator evaluator = new GeographicRuleEvaluator();

    private TransactionContext context(String country, String expected) {
        return new TransactionContext(
                UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                null, null, UUID.randomUUID(),
                country, expected, 0, Instant.now());
    }

    @Test
    void shouldMatchWhenCountryDiffersFromExpected() {
        FraudRule rule = FraudRule.create("GEOGRAPHIC_ANOMALY", FraudRule.RuleType.GEOGRAPHIC, 0, 30);
        RuleEvaluation result = evaluator.evaluate(rule, context("US", "ES"));

        assertTrue(result.matched());
        assertEquals(30, result.score());
    }

    @Test
    void shouldNotMatchWhenCountryMatchesExpected() {
        FraudRule rule = FraudRule.create("GEOGRAPHIC_ANOMALY", FraudRule.RuleType.GEOGRAPHIC, 0, 30);
        RuleEvaluation result = evaluator.evaluate(rule, context("ES", "ES"));

        assertFalse(result.matched());
    }

    @Test
    void shouldNotMatchWhenNoCountryData() {
        FraudRule rule = FraudRule.create("GEOGRAPHIC_ANOMALY", FraudRule.RuleType.GEOGRAPHIC, 0, 30);
        RuleEvaluation result = evaluator.evaluate(rule, context(null, null));

        assertFalse(result.matched());
        assertEquals(0, result.score());
        assertTrue(result.details().contains("No country data"));
    }

    @Test
    void shouldMatchCaseInsensitive() {
        FraudRule rule = FraudRule.create("GEOGRAPHIC_ANOMALY", FraudRule.RuleType.GEOGRAPHIC, 0, 30);
        RuleEvaluation result = evaluator.evaluate(rule, context("es", "ES"));

        assertFalse(result.matched());
    }
}
