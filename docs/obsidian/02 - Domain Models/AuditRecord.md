---
type: domain-model
service: aegis-audit-service
layer: domain
tags: [ddd, record, audit, compliance]
status: implemented
---

# AuditRecord

Domain record representing an audit entry for a financial transaction, ingested from Kafka.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier (UUID v7) |
| `walletId` | UUID | Wallet identifier |
| `userId` | UUID | User identifier |
| `amount` | BigDecimal | Amount deposited |
| `currency` | String | ISO 4217 currency code |
| `source` | String | Source of deposit (e.g., BANK_TRANSFER) |
| `reference` | String | Transaction reference |
| `newBalance` | BigDecimal | Wallet balance after deposit |
| `eventTimestamp` | Instant | Timestamp of the original event |
| `ingestedAt` | Instant | Timestamp when the record was ingested |
| `correlationId` | String | Correlation ID for distributed tracing |

## Factory Methods

- `AuditRecord.create(walletId, userId, amount, currency, source, reference, newBalance, eventTimestamp, ingestedAt, correlationId)` → new record

## Source Events

- [[03 - Domain Events/FundsDeposited|FundsDeposited]] consumed from `wallet.funds.deposited`

## Relationships

- Consumed by: [[04 - Ports/outbound/AuditRecordRepository|AuditRecordRepository]]
