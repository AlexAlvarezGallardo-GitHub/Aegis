package com.aegis.fraud.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class FraudAssessment {

    private final UUID assessmentId;
    private final UUID transactionId;
    private final String transactionType;
    private final int riskScore;
    private final FraudDecision decision;
    private final List<RuleEvaluation> rulesEvaluated;
    private final Instant timestamp;

    private FraudAssessment(UUID assessmentId, UUID transactionId, String transactionType,
                            int riskScore, FraudDecision decision,
                            List<RuleEvaluation> rulesEvaluated, Instant timestamp) {
        this.assessmentId = assessmentId;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.riskScore = riskScore;
        this.decision = decision;
        this.rulesEvaluated = List.copyOf(rulesEvaluated);
        this.timestamp = timestamp;
    }

    public static FraudAssessment complete(UUID transactionId, String transactionType,
                                           int riskScore, FraudDecision decision,
                                           List<RuleEvaluation> rulesEvaluated) {
        return new FraudAssessment(UUID.randomUUID(), transactionId, transactionType,
                riskScore, decision, rulesEvaluated, Instant.now());
    }

    public static FraudAssessment rehydrate(UUID assessmentId, UUID transactionId, String transactionType,
                                            int riskScore, FraudDecision decision,
                                            List<RuleEvaluation> rulesEvaluated, Instant timestamp) {
        return new FraudAssessment(assessmentId, transactionId, transactionType,
                riskScore, decision, rulesEvaluated, timestamp);
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public FraudDecision getDecision() {
        return decision;
    }

    public List<RuleEvaluation> getRulesEvaluated() {
        return rulesEvaluated;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
