package com.aegis.fraud.application.service;

import com.aegis.fraud.domain.model.FraudDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecisionMakerTest {

    private final DecisionMaker decisionMaker = new DecisionMaker(30, 70);

    @Test
    void shouldApproveBelowReviewThreshold() {
        assertEquals(FraudDecision.APPROVE, decisionMaker.decide(0));
        assertEquals(FraudDecision.APPROVE, decisionMaker.decide(29));
    }

    @Test
    void shouldReviewBetweenThresholds() {
        assertEquals(FraudDecision.REVIEW, decisionMaker.decide(30));
        assertEquals(FraudDecision.REVIEW, decisionMaker.decide(50));
        assertEquals(FraudDecision.REVIEW, decisionMaker.decide(70));
    }

    @Test
    void shouldRejectAboveRejectThreshold() {
        assertEquals(FraudDecision.REJECT, decisionMaker.decide(71));
        assertEquals(FraudDecision.REJECT, decisionMaker.decide(100));
    }
}
