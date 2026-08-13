---
type: domain-event
service: aegis-payment-service
layer: domain
tags: [event, kafka, payment]
status: implemented
topic: payment.requested
---

# PaymentRequested

Published when a payment is submitted and the `Payment` aggregate enters `PENDING`, before any fund movement.

```mermaid
graph LR
    Pay[Payment Service] -->|publishes| Topic[payment.requested]
    Topic --> Audit[Audit Service]
    style Pay fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Web as PaymentController
    participant Svc as ExecutePaymentService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    Web->>Svc: execute(PaymentCommand)
    Svc->>Svc: persist Payment (PENDING)
    Svc->>Pub: publish(PaymentRequested)
    Pub->>DB: INSERT outbox_event (payload=PaymentRequested JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group) → persist PaymentAuditRecord
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `PAYMENT_REQUESTED` |
| `schemaVersion` | String | `1.0` |
| `paymentId` | UUID | The payment's ID |
| `walletId` | UUID | Debit wallet |
| `userId` | UUID | Payer's ID |
| `amount` | BigDecimal | Payment amount |
| `currency` | String | ISO 4217 currency |
| `payee` | Object | `{name, id, type}` |
| `reference` | String | Idempotency key |
| `timestamp` | Instant | Event time |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Payment Service|Payment Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `payment.requested` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: `POST /api/v1/payments`
- **Consumed by**: [[01 - Services/Audit Service|Audit Service]] (persists [[02 - Domain Models/PaymentAuditRecord|PaymentAuditRecord]])
