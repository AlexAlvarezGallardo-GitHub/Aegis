---
type: value-object
service: aegis-fraud-service
layer: domain
tags: [ddd, enum, fraud, risk]
status: implemented
---

# FraudDecision

Enum representing the outcome of a fraud assessment.

## Values

| Value | Risk Score Band | Description |
|-------|-----------------|-------------|
| `APPROVE` | < 30 | Transaction cleared |
| `REVIEW` | 30 - 70 | Flagged for manual review |
| `REJECT` | > 70 | Transaction blocked |

## Decision Flow

```mermaid
stateDiagram-v2
    [*] --> ASSESSED: risk score computed
    ASSESSED --> APPROVE: score &lt; 30
    ASSESSED --> REVIEW: 30 &lt;= score &lt;= 70
    ASSESSED --> REJECT: score &gt; 70
    APPROVE --> [*]
    REVIEW --> [*]
    REJECT --> [*]
```

## Used By

- [[02 - Domain Models/FraudAssessment\|FraudAssessment]] aggregate root
- [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] event schema
