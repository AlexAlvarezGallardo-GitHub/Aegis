---
type: service
service: aegis-audit-service
layer: all
tags: [ddd, hexagonal, java, spring, audit]
status: implemented
port: 8088
database: aegis_audit
---

# Audit Service

**Purpose**: Persists immutable audit records of financial events for compliance, forensics, and regulatory reporting.

```mermaid
graph LR
    Kafka[("Kafka<br/>wallet.funds.deposited")] --> Consumer[FundsDepositedConsumer]
    Consumer --> Repo[AuditRecordRepository]
    Repo --> DB[(PostgreSQL<br/>aegis_audit)]
    subgraph "Hexagonal"
        Consumer
        Repo
    end
    style Kafka fill:#fdb,stroke:#333
    style DB fill:#afa,stroke:#333
```

```mermaid
sequenceDiagram
    participant Kafka as Apache Kafka
    participant Consumer as FundsDepositedConsumer
    participant Repo as AuditRecordRepository
    participant DB as PostgreSQL

    Kafka->>Consumer: FundsDeposited event (JSON)
    alt valid event
        Consumer->>Consumer: deserialize to FundsDepositedEvent
        Consumer->>Consumer: map to AuditRecord entity (set ingestedAt=now)
        Consumer->>Repo: save(auditRecord)
        Repo->>DB: INSERT audit_records
        Consumer->>Consumer: log at INFO
    else deserialization error
        Consumer->>Consumer: error handling (ErrorHandlingDeserializer)
    end
```

## Hexagonal Structure

### Domain (`com.aegis.audit.domain`)
- **Events**: `FundsDepositedEvent` (local copy for Kafka deserialization)
- **Models**: `AuditRecord` (JPA entity)

### Infrastructure (`com.aegis.audit.infrastructure`)
- **Persistence**: `AuditRecordRepository`
- **Messaging**: `FundsDepositedConsumer`
- **Config**: `KafkaConfig`

## Event Consumers

| Event | Topic | Handler | Action |
|-------|-------|---------|--------|
| [[03 - Domain Events/FundsDeposited\|FundsDeposited]] | `wallet.funds.deposited` | `FundsDepositedConsumer` | Persists `AuditRecord` with all event fields |

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Consumes from**: [[01 - Services/Wallet Service\|Wallet Service]] (via `wallet.funds.deposited`)
