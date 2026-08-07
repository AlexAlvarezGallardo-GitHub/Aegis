---
type: domain-model
service: aegis-audit-service
layer: domain
tags: [ddd, record, audit, fraud]
status: implemented
---

# FraudAuditRecord

Domain record capturing the outcome of a fraud assessment for audit and compliance purposes.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier (UUID v7) |
| `assessmentId` | UUID | Fraud assessment identifier |
| `transactionId` | UUID | Transaction identifier |
| `transactionType` | String | Type of transaction (e.g., TRANSFER, WITHDRAWAL) |
| `riskScore` | int | Computed risk score |
| `decision` | String | Fraud decision (APPROVE, REVIEW, REJECT) |
| `rulesEvaluated` | String | JSON representation of evaluated rules |
| `eventTimestamp` | Instant | Timestamp of the original event |
| `ingestedAt` | Instant | Timestamp when the record was ingested |

## Factory Methods

- `FraudAuditRecord.create(assessmentId, transactionId, transactionType, riskScore, decision, rulesEvaluated, eventTimestamp, ingestedAt)` → new record

## Source Events

- [[03 - Domain Events/FraudAssessmentCompleted|FraudAssessmentCompleted]] consumed from `fraud.assessment.completed`

## Relationships

- References: [[02 - Domain Models/FraudAssessment|FraudAssessment]] outcome, [[02 - Domain Models/RuleEvaluation|RuleEvaluation]] list (JSON)
- Consumed by: [[04 - Ports/outbound/FraudAuditRecordRepository|FraudAuditRecordRepository]]
