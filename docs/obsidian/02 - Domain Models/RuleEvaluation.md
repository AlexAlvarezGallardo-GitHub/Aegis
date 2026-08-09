---
type: value-object
service: aegis-fraud-service
layer: domain
tags: [ddd, value-object, fraud, rules]
status: implemented
---

# RuleEvaluation

Value object holding the result of evaluating a single [[02 - Domain Models/FraudRule|FraudRule]] against a transaction.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `ruleName` | String | Name of the evaluated rule |
| `score` | int | Score contributed by the rule |
| `matched` | boolean | Whether the rule triggered |
| `details` | String | Human-readable evaluation details |

## Used By

- [[02 - Domain Models/FraudAssessment|FraudAssessment]] aggregate root (`rulesEvaluated`)
- [[03 - Domain Events/FraudAssessmentCompleted|FraudAssessmentCompleted]] event schema
- [[02 - Domain Models/FraudAuditRecord|FraudAuditRecord]] (serialized as JSON)
