package com.aegis.audit.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAssessmentCompletedEvent(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID assessmentId,
        UUID transactionId,
        String transactionType,
        int riskScore,
        String decision,
        List<RuleEvaluation> rulesEvaluated,
        Instant timestamp
) {

    public record RuleEvaluation(String ruleName, int score, boolean matched, String details) {}
}
