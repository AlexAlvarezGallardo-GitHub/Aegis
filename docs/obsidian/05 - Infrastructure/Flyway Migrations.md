---
type: infrastructure
tags: [database, flyway, migration]
status: implemented
---

# Flyway Migrations

```mermaid
graph LR
    subgraph "Identity Service"
        IV1["V1__create_users_and_outbox_tables"]
        IV2["V2__add_auth_fields_to_users"]
        IV3["V3__create_refresh_tokens_table"]
        IV1 --> IV2 --> IV3
    end
    subgraph "Wallet Service"
        WV1["V1__create_wallet_tables"]
        WV2["V2__add_outbox_lock_index"]
        WV3["V3__unique_deposit_reference"]
        WV4["V4__add_ledger_reversal"]
        WV1 --> WV2 --> WV3 --> WV4
    end
    subgraph "Reporting Service"
        RV1["V1__create_balance_projection_table"]
        RV2["V2__create_processed_events_table"]
        RV1 --> RV2
    end
    subgraph "Audit Service"
        AV1["V1__create_audit_record_table"]
        AV2["V2__create_fraud_audit_records_table"]
        AV3["V3__create_processed_events_table"]
        AV1 --> AV2 --> AV3
    end
    subgraph "Fraud Service"
        FV1["V1__create_fraud_tables"]
        FV2["V2__create_outbox_events"]
        FV3["V3__create_processed_events_table"]
        FV1 --> FV2 --> FV3
    end
    subgraph "Databases"
        IDDB[(aegis_identity)]
        WDB[(aegis_wallet)]
        RDB[(aegis_reporting)]
        ADB[(aegis_audit)]
        FDB[(aegis_fraud)]
    end
    IV3 --> IDDB
    WV4 --> WDB
    RV2 --> RDB
    AV3 --> ADB
    FV3 --> FDB
    style IDDB fill:#afa,stroke:#333,color:#000
    style WDB fill:#afa,stroke:#333,color:#000
    style RDB fill:#afa,stroke:#333,color:#000
    style ADB fill:#afa,stroke:#333,color:#000
    style FDB fill:#afa,stroke:#333,color:#000
    style IV1 fill:#fdb,stroke:#333,color:#000
    style IV2 fill:#fdb,stroke:#333,color:#000
    style IV3 fill:#fdb,stroke:#333,color:#000
    style WV1 fill:#fdb,stroke:#333,color:#000
    style WV2 fill:#fdb,stroke:#333,color:#000
    style WV3 fill:#fdb,stroke:#333,color:#000
    style WV4 fill:#fdb,stroke:#333,color:#000
    style RV1 fill:#fdb,stroke:#333,color:#000
    style RV2 fill:#fdb,stroke:#333,color:#000
    style AV1 fill:#fdb,stroke:#333,color:#000
    style AV2 fill:#fdb,stroke:#333,color:#000
    style AV3 fill:#fdb,stroke:#333,color:#000
    style FV1 fill:#fdb,stroke:#333,color:#000
    style FV2 fill:#fdb,stroke:#333,color:#000
    style FV3 fill:#fdb,stroke:#333,color:#000
```

## Identity Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_users_and_outbox_tables.sql` | Initial users + outbox tables | ✅ |
| `V2__add_auth_fields_to_users.sql` | Add failed_login_attempts, locked_until | ✅ |
| `V3__create_refresh_tokens_table.sql` | Refresh token rotation table + indexes | ✅ |

## Wallet Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_wallet_tables.sql` | Wallets, ledger_entries, outbox_events | ✅ |
| `V2__add_outbox_lock_index.sql` | Rename status index + partial PENDING index for outbox relay | ✅ |
| `V3__unique_deposit_reference.sql` | Partial unique index on (wallet_id, reference) for DEPOSIT idempotency | ✅ |
| `V4__add_ledger_reversal.sql` | `reversal_of` column on ledger_entries + index | ✅ |

## Reporting Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_balance_projection_table.sql` | Balance projections read model | ✅ |
| `V2__create_processed_events_table.sql` | Processed events table (idempotency) + index | ✅ |

## Audit Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_audit_record_table.sql` | Audit records (deposit events) + indexes | ✅ |
| `V2__create_fraud_audit_records_table.sql` | Fraud audit records + indexes | ✅ |
| `V3__create_processed_events_table.sql` | Processed events table (idempotency) + index | ✅ |

## Fraud Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_fraud_tables.sql` | Fraud rules (seeded defaults) + fraud assessments | ✅ |
| `V2__create_outbox_events.sql` | Outbox events table + index | ✅ |
| `V3__create_processed_events_table.sql` | Processed events table (idempotency) + index | ✅ |
