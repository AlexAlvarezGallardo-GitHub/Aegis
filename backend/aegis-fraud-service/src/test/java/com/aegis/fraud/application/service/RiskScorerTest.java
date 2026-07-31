package com.aegis.fraud.application.service;

import com.aegis.fraud.domain.model.RuleEvaluation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskScorerTest {

    private final RiskScorer riskScorer = new RiskScorer();

    @Test
    void scoreShouldBeZeroWhenNoRulesMatch() {
        List<RuleEvaluation> evaluations = List.of(
                new RuleEvaluation("AMOUNT", 0, false, "within threshold"),
                new RuleEvaluation("VELOCITY", 0, false, "normal"));
        assertEquals(0, riskScorer.score(evaluations));
    }

    @Test
    void scoreShouldSumRuleScores() {
        List<RuleEvaluation> evaluations = List.of(
                new RuleEvaluation("AMOUNT", 30, true, "exceeds"),
                new RuleEvaluation("VELOCITY", 25, true, "high"));
        assertEquals(55, riskScorer.score(evaluations));
    }

    @Test
    void scoreShouldCapAtOneHundred() {
        List<RuleEvaluation> evaluations = List.of(
                new RuleEvaluation("AMOUNT", 50, true, "exceeds"),
                new RuleEvaluation("VELOCITY", 60, true, "high"),
                new RuleEvaluation("GEO", 40, true, "anomaly"));
        assertEquals(100, riskScorer.score(evaluations));
    }
}
