---
type: domain-event
service: aegis-identity-service
layer: domain
tags: [event, kafka, auth, security]
status: implemented
topic: aegis.identity.user-account-locked
---

# UserAccountLocked

Published when a user account is locked after 5 failed login attempts.

```mermaid
graph LR
    Identity[Identity Service] -->|publishes| Topic[aegis.identity.user-account-locked]
    style Identity fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Domain)
    participant Svc as AuthenticateUserService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic

    User->>Svc: authenticate()
    Svc->>Svc: failedAttempts >= 5 → lock account
    Svc->>Pub: publish(UserAccountLocked)
    Pub->>DB: INSERT outbox_event (payload=UserAccountLocked JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `USER_ACCOUNT_LOCKED` |
| `schemaVersion` | String | `1.0` |
| `userId` | UUID | Locked user's ID |
| `email` | String | User's email |
| `timestamp` | Instant | Event time |
| `failureCount` | int | Total failed attempts |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Identity Service|Identity Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `aegis.identity.user-account-locked` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: `User.authenticate()` when failed attempts reach 5

## Consumers

- Ninguno actualmente (sin consumidor configurado)
