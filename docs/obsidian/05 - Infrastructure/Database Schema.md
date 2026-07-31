---
type: infrastructure
tags: [database, postgresql, schema]
status: implemented
---

# Database Schema

```mermaid
graph TB
    subgraph "aegis_identity"
        Users["users<br/>id, email, password_hash<br/>status, failed_login_attempts<br/>locked_until, version"]
        OutboxI["outbox_events<br/>id, aggregate_id, event_type<br/>payload, topic, status"]
    end
    subgraph "aegis_wallet"
        Wallets["wallets<br/>id, user_id, name<br/>currency, balance<br/>reserved_balance, status, version"]
        Ledger["ledger_entries<br/>id, wallet_id, type<br/>amount, currency<br/>description, reference_id"]
        OutboxW["outbox_events<br/>id, aggregate_id, event_type<br/>payload, topic, status"]
    end
    Wallets -->|"1:N"| Ledger
    Users --> OutboxI
    Wallets --> OutboxW
    style Users fill:#afa,stroke:#333
    style Wallets fill:#afa,stroke:#333
    style Ledger fill:#fdb,stroke:#333
    style OutboxI fill:#bbf,stroke:#333
    style OutboxW fill:#bbf,stroke:#333
```

## Identity Database (`aegis_identity`)

### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL |
| `password_hash` | VARCHAR(60) | NOT NULL |
| `status` | VARCHAR(30) | NOT NULL |
| `failed_login_attempts` | INT | DEFAULT 0 |
| `locked_until` | TIMESTAMP | NULLABLE |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |
| `version` | INT | Optimistic lock |

### `outbox_events`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `aggregate_id` | UUID | NOT NULL |
| `event_type` | VARCHAR(255) | NOT NULL |
| `payload` | JSONB | NOT NULL |
| `topic` | VARCHAR(255) | NOT NULL |
| `status` | VARCHAR(20) | DEFAULT 'PENDING' |
| `created_at` | TIMESTAMP | NOT NULL |
| `published_at` | TIMESTAMP | NULLABLE |

## Wallet Database (`aegis_wallet`)

### `wallets`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `user_id` | UUID | NOT NULL, INDEX |
| `name` | VARCHAR(100) | NULLABLE |
| `currency` | VARCHAR(3) | NOT NULL |
| `balance` | DECIMAL(19,4) | NOT NULL |
| `reserved_balance` | DECIMAL(19,4) | DEFAULT 0 |
| `status` | VARCHAR(20) | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |
| `version` | INT | Optimistic lock |

### `ledger_entries`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK |
| `wallet_id` | UUID | FK → wallets.id |
| `type` | VARCHAR(20) | NOT NULL |
| `amount` | DECIMAL(19,4) | NOT NULL |
| `currency` | VARCHAR(3) | NOT NULL |
| `description` | TEXT | NULLABLE |
| `reference_id` | VARCHAR(255) | NULLABLE |
| `created_at` | TIMESTAMP | NOT NULL |

### `outbox_events`
Same structure as identity's outbox table.
