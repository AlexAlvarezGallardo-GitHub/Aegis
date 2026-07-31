---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet, deposit]
status: implemented
topic: wallet.funds.deposited
---

# FundsDeposited

Published when funds are deposited into a wallet via the dedicated deposit endpoint.

```mermaid
graph LR
    Wallet[Wallet Service] -->|publishes| Topic[wallet.funds.deposited]
    Topic --> Report[Reporting Service]
    Topic --> Audit[Audit Service]
    style Wallet fill:#bbf,stroke:#333
    style Topic fill:#fdb,stroke:#333
    style Report fill:#bfb,stroke:#333
    style Audit fill:#bfb,stroke:#333
```

```mermaid
sequenceDiagram
    participant Wallet as Wallet Domain
    participant Svc as DepositFundsService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Report as Reporting Consumer
    participant Audit as Audit Consumer

    Wallet->>Svc: depositFunds()
    Svc->>Pub: publish(FundsDeposited)
    Pub->>DB: INSERT outbox_event (payload=FundsDeposited JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Report: Consume (group=reporting-group)
    Kafka->>Audit: Consume (group=audit-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `FUNDS_DEPOSITED` |
| `schemaVersion` | String | `1.0` |
| `walletId` | UUID | Target wallet's ID |
| `userId` | UUID | Owner's ID |
| `amount` | BigDecimal | Deposited amount (positive) |
| `currency` | String | ISO 4217 currency |
| `source` | String | Source of funds (BANK_TRANSFER, CARD, etc.) |
| `reference` | String | External reference for idempotency |
| `newBalance` | BigDecimal | Wallet balance after deposit |
| `timestamp` | Instant | Event time |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Wallet Service\|Wallet Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `wallet.funds.deposited` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Trigger**: `POST /api/v1/wallets/{id}/deposits`
- **Consumed by**: [[01 - Services/Reporting Service\|Reporting Service]], [[01 - Services/Audit Service\|Audit Service]]
