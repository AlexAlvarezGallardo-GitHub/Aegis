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
        IV1 --> IV2
    end
    subgraph "Wallet Service"
        WV1["V1__create_wallet_tables"]
    end
    subgraph "Databases"
        IDDB[(aegis_identity)]
        WDB[(aegis_wallet)]
    end
    IV2 --> IDDB
    WV1 --> WDB
    style IDDB fill:#afa,stroke:#333,color:#000
    style WDB fill:#afa,stroke:#333,color:#000
    style IV1 fill:#fdb,stroke:#333,color:#000
    style IV2 fill:#fdb,stroke:#333,color:#000
    style WV1 fill:#fdb,stroke:#333,color:#000
```

## Identity Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_users_and_outbox_tables.sql` | Initial users + outbox tables | ✅ |
| `V2__add_auth_fields_to_users.sql` | Add failed_login_attempts, locked_until | ✅ |

## Wallet Service

| File | Description | Applied |
|------|-------------|---------|
| `V1__create_wallet_tables.sql` | Wallets, ledger_entries, outbox_events | ✅ |
