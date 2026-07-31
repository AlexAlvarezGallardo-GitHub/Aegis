package com.aegis.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_audit_records")
public class FraudAuditRecord {

    @Id
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "decision", nullable = false)
    private String decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_evaluated", columnDefinition = "jsonb", nullable = false)
    private String rulesEvaluatedJson;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected FraudAuditRecord() {}

    public FraudAuditRecord(UUID id, UUID assessmentId, UUID transactionId, String transactionType,
                            int riskScore, String decision, String rulesEvaluatedJson,
                            Instant eventTimestamp, Instant ingestedAt) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.riskScore = riskScore;
        this.decision = decision;
        this.rulesEvaluatedJson = rulesEvaluatedJson;
        this.eventTimestamp = eventTimestamp;
        this.ingestedAt = ingestedAt;
    }

    public UUID getId() {
        return id;
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

    public String getDecision() {
        return decision;
    }

    public String getRulesEvaluatedJson() {
        return rulesEvaluatedJson;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
