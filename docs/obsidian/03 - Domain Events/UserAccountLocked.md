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
    Topic --> Notification[Notification Service]
    Topic --> Audit[Audit Service]
    style Identity fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Notification fill:#bfb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Domain)
    participant Svc as AuthenticateUserService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Notification as Notification Consumer
    participant Audit as Audit Consumer

    User->>Svc: recordFailedAttempt()
    Svc->>Svc: attempts >= 5 → lock account
    Svc->>Pub: publish(UserAccountLocked)
    Pub->>DB: INSERT outbox_event (payload=UserAccountLocked JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Notification: Consume (group=notification-group)
    Kafka->>Audit: Consume (group=audit-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | Locked user's ID |
| `email` | String | User's email |
| `failedAttempts` | int | Total failed attempts |
| `lockedUntil` | Instant | Lock expiry |
| `timestamp` | Instant | Event time |

## Details

- **Producer**: [[01 - Services/Identity Service\|Identity Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.identity.user-account-locked` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Schema**: `specs/002-user-authentication/contracts/user-account-locked-event.json`
- **Trigger**: `User.recordFailedAttempt()` when attempts reach 5

## Consumers

- Notification service (future) — send alert email
- Audit service (future)
