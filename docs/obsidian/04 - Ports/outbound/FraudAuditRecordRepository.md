---
type: port
service: aegis-audit-service
layer: domain
tags: [port, outbound, repository, audit, fraud]
status: implemented
port-type: outbound
---

# FraudAuditRecordRepository

Outbound port for persisting and querying fraud audit records.

## Methods

| Method | Description |
|--------|-------------|
| `save(record)` → `FraudAuditRecord` | Persist a [[02 - Domain Models/FraudAuditRecord\|FraudAuditRecord]] |

## Implementation

- **Adapter**: in `infrastructure/persistence/`
- **Consumer**: writes records when [[03 - Domain Events/FraudAssessmentCompleted|FraudAssessmentCompleted]] is consumed from `fraud.assessment.completed`

## Used By

- `FraudAssessmentConsumer` in [[01 - Services/Audit Service|Audit Service]]
