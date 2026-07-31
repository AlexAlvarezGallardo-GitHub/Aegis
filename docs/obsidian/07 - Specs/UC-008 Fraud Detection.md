---
type: spec
tags: [spec, uc-008, fraud, risk]
status: draft
feature-branch: feature/008-fraud-detection
---

# UC-008: Fraud Detection

**Feature Branch**: `feature/008-fraud-detection`

## Overview

Real-time fraud detection engine that evaluates transactions against configurable rules, calculates a composite risk score (0-100), and issues APPROVE/REVIEW/REJECT decisions.

## Scope

- **Fraud Service** (new, port 8089): rules engine, risk scorer, decision maker, Kafka pub/sub, sync assessment endpoint
- **Audit Service**: consumes `FraudAssessmentCompleted` → persists `FraudAuditRecord`

## Business Rules

1. Score < 30 → **APPROVE**
2. Score 30-70 → **REVIEW**
3. Score > 70 → **REJECT**
4. Rules DB-configurable (ADR-001)
5. Sync assessment < 200ms

## Rules (defaults)

| Rule | Type | Threshold | Weight |
|------|------|-----------|--------|
| VELOCITY_CHECK | VELOCITY | 5 | 25 |
| AMOUNT_THRESHOLD | AMOUNT | 1000 | 30 |
| GEOGRAPHIC_ANOMALY | GEOGRAPHIC | 0 | 30 |
| OFF_HOURS_TRANSACTION | TIME | 0 | 15 |

## Events

| Direction | Event | Topic |
|-----------|-------|-------|
| Produced | [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] | `fraud.assessment.completed` |
| Consumed | TransferRequested | `payment.transfer.requested` |

## API

- `POST /api/v1/fraud/assess` — sync assessment
- `GET /api/v1/fraud/assessments/{id}` — retrieve assessment

## Implementation

See `specs/008-fraud-detection/` for full spec, plan, tasks, and contracts.
