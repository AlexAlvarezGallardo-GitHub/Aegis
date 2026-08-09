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

**Purpose**: Persists immutable audit records of financial events for compliance, forensics, and regulatory reporting. **Passive service** — it has no REST endpoints and produces no events.

```mermaid
graph LR
    subgraph Hexagonal["Hexagonal"]
        Consumer1["FundsDepositedConsumer"]
        Consumer2["FraudAssessmentConsumer"]
        Repo1["AuditRecordRepository"]
        Repo2["FraudAuditRecordRepository"]
        Consumer1 --> Repo1
        Consumer2 --> Repo2
    end
    Kafka["Kafka"] --> Consumer1
    Kafka --> Consumer2
    Repo1 --> DB[("PostgreSQL<br/>aegis_audit")]
    Repo2 --> DB
    style Consumer1 fill:#bbf,stroke:#333,color:#000
    style Consumer2 fill:#bbf,stroke:#333,color:#000
    style Repo1 fill:#fdb,stroke:#333,color:#000
    style Repo2 fill:#fdb,stroke:#333,color:#000
    style Kafka fill:#fdb,stroke:#333,color:#000
    style DB fill:#afa,stroke:#333,color:#000
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
        Consumer->>Consumer: check processed_events (idempotency)
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
- **Events**: `FundsDepositedEvent`, `FraudAssessmentCompletedEvent` (local copies for Kafka deserialization)
- **Models**: `AuditRecord`, `FraudAuditRecord`

### Infrastructure (`com.aegis.audit.infrastructure`)
- **Persistence**: `AuditRecordRepository`, `FraudAuditRecordRepository`, `ProcessedEventJpaRepository`
- **Messaging**: `FundsDepositedConsumer`, `FraudAssessmentConsumer`
- **Config**: `KafkaConfig`

## Event Consumers

| Event | Topic | Handler | Action |
|-------|-------|---------|--------|
| [[03 - Domain Events/FundsDeposited\|FundsDeposited]] | `wallet.funds.deposited` | `FundsDepositedConsumer` | Persists `AuditRecord` with all event fields |
| [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] | `fraud.assessment.completed` | `FraudAssessmentConsumer` | Persists `FraudAuditRecord` |

Consumption is idempotent via the `processed_events` table (deduplication by event ID).

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Consumes from**: [[01 - Services/Wallet Service\|Wallet Service]] (via `wallet.funds.deposited`)
- **Consumes from**: [[01 - Services/Fraud Service\|Fraud Service]] (via `fraud.assessment.completed`)

## Flyway Migrations

| File | Description |
|------|-------------|
| `V1__create_audit_record_table.sql` | Audit records |
| `V2__create_fraud_audit_records_table.sql` | Fraud audit records |
| `V3__create_processed_events_table.sql` | Processed events (idempotency) |
