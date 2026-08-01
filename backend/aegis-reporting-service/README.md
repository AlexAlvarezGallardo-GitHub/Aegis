# Reporting Service

**Purpose**: Maintains denormalized read models for reporting and dashboards by consuming domain events.

## Functionality

- Consumes `FundsDeposited` events from Kafka topic `wallet.funds.deposited`
- Upserts `BalanceProjection` records (read-model) by wallet ID
- Provides a queryable balance view aggregated from deposit events

## Architecture

```mermaid
graph LR
    Kafka[("Kafka<br/>wallet.funds.deposited")] --> Consumer["FundsDepositedConsumer"]
    Consumer --> Repo["BalanceProjectionRepository"]
    Repo --> DB[("PostgreSQL<br/>aegis_reporting")]
    style Kafka fill:#fdb,stroke:#333,color:#000
    style DB fill:#afa,stroke:#333,color:#000
```

## Tech Stack

- Java 21, Spring Boot 3.3, Spring Kafka
- PostgreSQL, Flyway migrations
- Testcontainers for integration tests

## Configuration

| Property | Value |
|----------|-------|
| Port | 8087 |
| Database | `aegis_reporting` |
| Kafka consumer group | `reporting-group` |

## Event Consumers

| Event | Topic | Action |
|-------|-------|--------|
| FundsDeposited | `wallet.funds.deposited` | Upserts BalanceProjection |
