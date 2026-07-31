package com.aegis.fraud.domain.event;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.model.RuleEvaluation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAssessmentCompleted(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID assessmentId,
        UUID transactionId,
        String transactionType,
        int riskScore,
        FraudDecision decision,
        List<RuleEvaluation> rulesEvaluated,
        Instant timestamp
) {

    private static final String EVENT_TYPE = "FRAUD_ASSESSMENT_COMPLETED";
    private static final String SCHEMA_VERSION = "1.0";

    public FraudAssessmentCompleted(FraudAssessment assessment) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                assessment.getAssessmentId(),
                assessment.getTransactionId(),
                assessment.getTransactionType(),
                assessment.getRiskScore(),
                assessment.getDecision(),
                assessment.getRulesEvaluated(),
                assessment.getTimestamp()
        );
    }
}
