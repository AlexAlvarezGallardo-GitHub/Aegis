---
type: port
service: aegis-fraud-service
layer: domain
tags: [port, outbound, repository, fraud]
status: implemented
port-type: outbound
---

# FraudAssessmentRepository

Outbound port for persisting and retrieving fraud assessments.

## Methods

| Method | Description |
|--------|-------------|
| `save(assessment)` → `FraudAssessment` | Persist an assessment |
| `findById(assessmentId)` → `Optional<FraudAssessment>` | Lookup by assessment ID |

## Implementation

- **Adapter**: in `infrastructure/persistence/`
- **Entity**: [[02 - Domain Models/FraudAssessment|FraudAssessment]] aggregate

## Used By

- [[04 - Ports/inbound/AssessFraudUseCase|AssessFraudUseCase]]
