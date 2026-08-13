---
type: domain-event
service: aegis-payment-service
layer: domain
tags: [event, kafka, payment]
status: implemented
topic: payment.executed
---

# PaymentExecuted

Published when a payment settles: the wallet is debited and the `Payment` aggregate reaches `COMPLETED`.

```mermaid
graph LR
    Pay[Payment Service] -->|publishes| Topic[payment.executed]
    Topic --> Audit[Audit Service]
    Topic --> Report[Reporting Service]
    style Pay fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
    style Report fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Svc as ExecutePaymentService
    participant Wallet as WalletGateway
    participant DB as PostgreSQL
    participant Pub as KafkaEventPublisher
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    Svc->>Wallet: createHold(paymentId)
    Svc->>Wallet: debitHold(paymentId, holdId)
    Svc->>Svc: Payment → COMPLETED
    Svc->>Pub: publish(PaymentExecuted)
    Pub->>DB: INSERT outbox_event
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group) → persist PaymentAuditRecord
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `PAYMENT_EXECUTED` |
| `schemaVersion` | String | `1.0` |
| `paymentId` | UUID | The payment's ID |
| `walletId` | UUID | Debited wallet |
| `userId` | UUID | Payer's ID |
| `amount` | BigDecimal | Payment amount |
| `currency` | String | ISO 4217 currency |
| `payee` | Object | `{name, id, type}` |
| `fraudAssessmentId` | UUID | Fraud assessment that authorized it |
| `holdId` | UUID | Settled hold |
| `newBalance` | BigDecimal | Wallet balance after payment |
| `reference` | String | Idempotency key |
| `timestamp` | Instant | Event time |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Payment Service|Payment Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `payment.executed` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: successful `POST /api/v1/payments` (saga: fraud → hold → debit)
- **Consumed by**: [[01 - Services/Audit Service|Audit Service]] (persists [[02 - Domain Models/PaymentAuditRecord|PaymentAuditRecord]])
