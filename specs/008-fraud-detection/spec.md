# Feature Specification: UC-008 Fraud Detection

**Feature Branch**: `feature/008-fraud-detection`

**Created**: 2026-07-31

**Status**: Draft

---

## Problem

Transactions must be evaluated against fraud rules in real time to flag or block risky operations before execution. The platform needs a configurable rules engine with risk scoring and approve/review/reject decisions.

## Solution

A dedicated **Fraud Service** that:
- Consumes transaction events from Kafka (`payment.transfer.requested`, `payment.executed`)
- Evaluates transactions against configurable rules (velocity, amount threshold, geographic, time-based)
- Aggregates rule scores into a composite risk score (0-100)
- Generates a decision: APPROVE (score < 30), REVIEW (30-70), REJECT (> 70)
- Publishes `FraudAssessmentCompleted` to Kafka
- Exposes sync `POST /api/v1/fraud/assess` for real-time calls

### Business Rules
1. Risk score 0-100 composite of weighted rule evaluations
2. Score < 30 → **APPROVE**
3. Score 30-70 → **REVIEW**
4. Score > 70 → **REJECT**
5. Rules are configurable via DB (no code changes to add rules)
6. Assessment must complete in under 200ms

---

## Affected Services

| Service | Role |
|---------|------|
| **aegis-fraud-service** | New service: rules engine, risk scorer, assessment endpoints, Kafka pub/sub |
| **aegis-audit-service** | Consume `FraudAssessmentCompleted` → persist audit record |

---

## Architecture

```
Payment/Wallet ──(Kafka)──→ Fraud Service
                              │
                              ├── Rules Engine (domain/model)
                              ├── Risk Scorer (application/service)
                              ├── POST /api/v1/fraud/assess (sync)
                              │
                              └──→ Kafka: fraud.assessment.completed
                                        │
                                        └──→ Audit Service
```

---

## API

### POST /api/v1/fraud/assess

Synchronous assessment for real-time calls.

**Request**:
```json
{
  "transactionId": "uuid",
  "transactionType": "TRANSFER",
  "amount": 500.00,
  "currency": "EUR",
  "sourceWalletId": "uuid",
  "destWalletId": "uuid",
  "userId": "uuid"
}
```

**Response (200)**:
```json
{
  "assessmentId": "uuid",
  "transactionId": "uuid",
  "riskScore": 45,
  "decision": "REVIEW",
  "rulesEvaluated": [
    { "ruleName": "VELOCITY", "score": 20, "matched": true, "details": "3 transactions in 10 minutes" }
  ],
  "timestamp": "2026-07-31T12:00:00Z"
}
```

### GET /api/v1/fraud/assessments/{id}

Retrieve a stored assessment.

---

## Domain Model

- **FraudAssessment** — aggregate (id, transactionId, transactionType, riskScore, decision, rulesEvaluated, timestamp)
- **FraudDecision** — enum: APPROVE, REVIEW, REJECT
- **FraudRule** — configurable rule (id, name, type, threshold, weight, enabled)
- **RuleEvaluation** — value object (ruleName, score, matched, details)

---

## Events

### Produced: FraudAssessmentCompleted
- `assessmentId: UUID`
- `transactionId: UUID`
- `riskScore: int`
- `decision: FraudDecision`
- `rulesEvaluated: List<RuleEvaluation>`
- `timestamp: Instant`

Topic: `fraud.assessment.completed`

### Consumed
- `TransferRequestedEvent` from `payment.transfer.requested`
- `PaymentExecutedEvent` from `payment.executed`

---

## Sub-Tasks

- [ ] Spec + OpenAPI contract
- [ ] Domain layer (FraudAssessment, FraudDecision, FraudRule, RuleEvaluation)
- [ ] Ports (AssessFraudUseCase, FraudRuleRepository)
- [ ] Rules engine + risk scorer
- [ ] DTOs + mappers
- [ ] Kafka consumer + producer
- [ ] JPA persistence
- [ ] FraudController
- [ ] Audit consumer
- [ ] Tests
- [ ] ADR
