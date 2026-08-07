---
type: domain-model
service: aegis-fraud-service
layer: domain
tags: [ddd, aggregate, fraud, risk]
status: implemented
---

# FraudAssessment

Aggregate root for the Fraud bounded context. Captures the outcome of scoring a transaction against the fraud rules engine.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `assessmentId` | UUID | Unique assessment identifier (UUID v7) |
| `transactionId` | UUID | Assessed transaction's ID |
| `transactionType` | String | TRANSFER, PAYMENT, etc. |
| `riskScore` | int | Composite risk score 0-100 |
| `decision` | [[02 - Domain Models/FraudDecision\|FraudDecision]] | APPROVE, REVIEW, REJECT |
| `rulesEvaluated` | List<[[02 - Domain Models/RuleEvaluation\|RuleEvaluation]]> | Per-rule scores and details |
| `timestamp` | Instant | Assessment time |

## Factory Methods

- `FraudAssessment.complete(transactionId, transactionType, riskScore, decision, rulesEvaluated)` → new assessment
- `FraudAssessment.rehydrate(assessmentId, transactionId, transactionType, riskScore, decision, rulesEvaluated, timestamp)` → reconstituted aggregate

## Domain Events Published

- [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] (on completion)

## Relationships

- Contains: [[02 - Domain Models/FraudDecision\|FraudDecision]], [[02 - Domain Models/RuleEvaluation\|RuleEvaluation]]
- Evaluated against: [[02 - Domain Models/FraudRule\|FraudRule]] rules
- Consumed by: [[04 - Ports/outbound/FraudAssessmentRepository\|FraudAssessmentRepository]]

## Business Rules

- Risk score ranges 0-100
- Decision thresholds: APPROVE < 30, REVIEW 30-70, REJECT > 70
