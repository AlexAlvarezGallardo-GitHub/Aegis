---
type: port
service: aegis-fraud-service
layer: domain
tags: [port, outbound, repository, fraud, rules]
status: implemented
port-type: outbound
---

# FraudRuleRepository

Outbound port for retrieving enabled fraud rules.

## Methods

| Method | Description |
|--------|-------------|
| `findEnabledRules()` → `List<FraudRule>` | Load all rules with `enabled = true` |

## Implementation

- **Adapter**: in `infrastructure/persistence/`
- **Entity**: [[02 - Domain Models/FraudRule|FraudRule]] value object

## Used By

- [[04 - Ports/inbound/AssessFraudUseCase|AssessFraudUseCase]]
