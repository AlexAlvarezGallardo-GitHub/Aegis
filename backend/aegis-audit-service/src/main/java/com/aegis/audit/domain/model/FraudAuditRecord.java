package com.aegis.audit.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a fraud audit record.
 *
 * <p>Captures the outcome of a fraud assessment for a transaction,
 * including the risk score, decision, and rules evaluated.</p>
 *
 * @param id               unique identifier (UUIDv7)
 * @param assessmentId     fraud assessment identifier
 * @param transactionId    transaction identifier
 * @param transactionType  type of transaction (e.g., TRANSFER, WITHDRAWAL)
 * @param riskScore        computed risk score
 * @param decision         fraud decision (e.g., APPROVED, REJECTED, REVIEW)
 * @param rulesEvaluated   JSON representation of evaluated rules
 * @param eventTimestamp   timestamp of the original event
 * @param ingestedAt       timestamp when the record was ingested
 */
public record FraudAuditRecord(
        UUID id,
        UUID assessmentId,
        UUID transactionId,
        String transactionType,
        int riskScore,
        String decision,
        String rulesEvaluated,
        Instant eventTimestamp,
        Instant ingestedAt
) {

    /**
     * Factory method that creates a new FraudAuditRecord with a generated UUIDv7 identifier.
     *
     * @param assessmentId     fraud assessment identifier
     * @param transactionId    transaction identifier
     * @param transactionType  type of transaction
     * @param riskScore        computed risk score
     * @param decision         fraud decision
     * @param rulesEvaluated   JSON representation of evaluated rules
     * @param eventTimestamp   timestamp of the original event
     * @param ingestedAt       timestamp when the record was ingested
     * @return a new FraudAuditRecord instance
     */
    public static FraudAuditRecord create(
            UUID assessmentId,
            UUID transactionId,
            String transactionType,
            int riskScore,
            String decision,
            String rulesEvaluated,
            Instant eventTimestamp,
            Instant ingestedAt
    ) {
        Objects.requireNonNull(assessmentId, "assessmentId must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(transactionType, "transactionType must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(rulesEvaluated, "rulesEvaluated must not be null");
        Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
        Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");

        return new FraudAuditRecord(
                UuidV7Generator.generate(),
                assessmentId,
                transactionId,
                transactionType,
                riskScore,
                decision,
                rulesEvaluated,
                eventTimestamp,
                ingestedAt
        );
    }
}
