package com.aegis.fraud.application.dto;

import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.RuleEvaluation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentResponse(
        UUID assessmentId,
        UUID transactionId,
        String transactionType,
        int riskScore,
        String decision,
        List<RuleEvaluation> rulesEvaluated,
        Instant timestamp
) {

    public static AssessmentResponse from(FraudAssessment assessment) {
        return new AssessmentResponse(
                assessment.getAssessmentId(),
                assessment.getTransactionId(),
                assessment.getTransactionType(),
                assessment.getRiskScore(),
                assessment.getDecision().name(),
                assessment.getRulesEvaluated(),
                assessment.getTimestamp()
        );
    }
}
