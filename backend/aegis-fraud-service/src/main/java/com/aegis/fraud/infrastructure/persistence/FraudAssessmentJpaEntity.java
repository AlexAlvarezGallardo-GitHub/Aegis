package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.domain.model.RuleEvaluation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessmentJpaEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudDecisionJpa decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_evaluated", columnDefinition = "jsonb", nullable = false)
    private String rulesEvaluatedJson;

    @Column(nullable = false)
    private Instant timestamp;

    protected FraudAssessmentJpaEntity() {}

    public FraudAssessmentJpaEntity(UUID id, UUID transactionId, String transactionType,
                                    int riskScore, FraudDecisionJpa decision,
                                    List<RuleEvaluation> rulesEvaluated, Instant timestamp) {
        this.id = id;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.riskScore = riskScore;
        this.decision = decision;
        this.rulesEvaluatedJson = toJson(rulesEvaluated);
        this.timestamp = timestamp;
    }

    public enum FraudDecisionJpa {
        APPROVE,
        REVIEW,
        REJECT
    }

    private static String toJson(List<RuleEvaluation> evaluations) {
        try {
            return MAPPER.writeValueAsString(evaluations);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rule evaluations", e);
        }
    }

    public List<RuleEvaluation> toRuleEvaluations() {
        try {
            return MAPPER.readValue(rulesEvaluatedJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize rule evaluations", e);
        }
    }

    public UUID getId() {
        return id;
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

    public FraudDecisionJpa getDecision() {
        return decision;
    }

    public String getRulesEvaluatedJson() {
        return rulesEvaluatedJson;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public static List<RuleEvaluation> emptyEvaluations() {
        return new ArrayList<>();
    }
}
