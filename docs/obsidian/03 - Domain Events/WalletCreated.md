---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet]
status: implemented
topic: aegis.wallet.created
---

# WalletCreated

Published when a new wallet is created.

```mermaid
graph LR
    Wallet[Wallet Service] -->|publishes| Topic[aegis.wallet.created]
    style Wallet fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Wallet as Wallet (Domain)
    participant Svc as CreateWalletService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic

    Wallet->>Svc: create()
    Svc->>Pub: publish(WalletCreated)
    Pub->>DB: INSERT outbox_event (payload=WalletCreated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `WALLET_CREATED` |
| `schemaVersion` | String | `1.0` |
| `walletId` | UUID | New wallet's ID |
| `userId` | UUID | Owner's ID |
| `currency` | String | ISO 4217 currency |
| `timestamp` | Instant | Event time |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Wallet Service|Wallet Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `aegis.wallet.created` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: `Wallet.create()` factory method

## Consumers

- Ninguno actualmente (sin consumidor configurado)
