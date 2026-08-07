---
type: port
service: aegis-audit-service
layer: domain
tags: [port, outbound, repository, audit]
status: implemented
port-type: outbound
---

# AuditRecordRepository

Outbound port for persisting and querying financial audit records.

## Methods

| Method | Description |
|--------|-------------|
| `save(record)` → `AuditRecord` | Persist an [[02 - Domain Models/AuditRecord\|AuditRecord]] |

## Implementation

- **Adapter**: in `infrastructure/persistence/`
- **Consumer**: writes records when [[03 - Domain Events/FundsDeposited|FundsDeposited]] is consumed from `wallet.funds.deposited`

## Used By

- `FundsDepositedConsumer` in [[01 - Services/Audit Service|Audit Service]]
