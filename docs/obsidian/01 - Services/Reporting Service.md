---
type: service
service: aegis-reporting-service
layer: all
tags: [ddd, hexagonal, java, spring, reporting]
status: implemented
port: 8087
database: aegis_reporting
---

# Reporting Service

**Purpose**: Consumes domain events to maintain a denormalized read-model. It upserts a `BalanceProjection` per wallet from `wallet.funds.deposited` events. **Passive service** — it has no REST endpoints and produces no events.

```mermaid
graph LR
    subgraph Hexagonal["Hexagonal"]
        Consumer["FundsDepositedConsumer"]
        Repo["BalanceProjectionRepository"]
        Consumer --> Repo
    end
    Kafka[("Kafka<br/>wallet.funds.deposited")] --> Consumer
    Repo --> DB[("PostgreSQL<br/>aegis_reporting")]
    style Consumer fill:#bbf,stroke:#333,color:#000
    style Repo fill:#fdb,stroke:#333,color:#000
    style Kafka fill:#fdb,stroke:#333,color:#000
    style DB fill:#afa,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Kafka as Apache Kafka
    participant Consumer as FundsDepositedConsumer
    participant Repo as BalanceProjectionRepository
    participant DB as PostgreSQL

    Kafka->>Consumer: FundsDeposited event
    Consumer->>Consumer: check processed_events (idempotency)
    Consumer->>Repo: findByWalletId(walletId)
    alt exists
        Repo-->>Consumer: BalanceProjection
        Consumer->>Consumer: updateBalance(newBalance, timestamp)
    else not found
        Repo-->>Consumer: empty
        Consumer->>Consumer: create new BalanceProjection
    end
    Consumer->>Repo: save(projection)
    Repo->>DB: UPSERT balance_projections
    Consumer->>Consumer: log at INFO
```

## Hexagonal Structure

### Domain (`com.aegis.reporting.domain`)
- **Events**: `FundsDepositedEvent` (local copy for Kafka deserialization)
- **Models**: `BalanceProjection` (read-model)

### Infrastructure (`com.aegis.reporting.infrastructure`)
- **Persistence**: `BalanceProjectionRepository`, `ProcessedEventJpaRepository`
- **Messaging**: `FundsDepositedConsumer`
- **Config**: `KafkaConfig`

## Event Consumers

| Event | Topic | Handler | Action |
|-------|-------|---------|--------|
| [[03 - Domain Events/FundsDeposited\|FundsDeposited]] | `wallet.funds.deposited` | `FundsDepositedConsumer` | Upserts `BalanceProjection` by walletId |

Consumption is idempotent via the `processed_events` table (deduplication by event ID).

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Consumes from**: [[01 - Services/Wallet Service\|Wallet Service]] (via `wallet.funds.deposited`)

## Flyway Migrations

| File | Description |
|------|-------------|
| `V1__create_balance_projection_table.sql` | Balance projections |
| `V2__create_processed_events_table.sql` | Processed events (idempotency) |
