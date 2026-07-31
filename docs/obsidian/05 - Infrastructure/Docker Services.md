---
type: infrastructure
tags: [docker, devops, infrastructure]
status: implemented
---

# Docker Services

Local development infrastructure provisioned via Docker Compose.

```mermaid
graph TB
    subgraph "Databases"
        PG_I["postgres-identity :5432"]
        PG_W["postgres-wallet :5433"]
        PG_R["postgres-reporting :5434"]
        PG_A["postgres-audit :5435"]
        PG_F["postgres-fraud :5436"]
        Redis["redis :6379"]
    end
    subgraph "Messaging"
        ZK["zookeeper :2181"]
        Kafka["kafka :9092"]
        KUI["kafka-ui :8090"]
    end
    subgraph "Applications"
        Identity["aegis-identity :8081"]
        Wallet["aegis-wallet :8083"]
        BFF["aegis-bff :8082"]
        Frontend["aegis-frontend :4200"]
    end
    subgraph "Admin"
        DbGate["dbgate :3000"]
    end
    Identity --> PG_I
    Wallet --> PG_W
    BFF --> Redis
    Frontend --> BFF
    Kafka --> ZK
    KUI --> Kafka
    DbGate --> PG_I & PG_W & PG_R & PG_A & PG_F & Redis
    style PG_I fill:#afa,stroke:#333
    style PG_W fill:#afa,stroke:#333
    style PG_R fill:#afa,stroke:#333
    style PG_A fill:#afa,stroke:#333
    style PG_F fill:#afa,stroke:#333
    style Redis fill:#fdb,stroke:#333
    style Identity fill:#bbf,stroke:#333
    style Wallet fill:#bbf,stroke:#333
    style BFF fill:#bbf,stroke:#333
    style Frontend fill:#bbf,stroke:#333
```

## Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `postgres-identity` | postgres:16-alpine | 5432 | Identity DB |
| `postgres-wallet` | postgres:16-alpine | 5433 | Wallet DB |
| `zookeeper` | confluentinc/cp-zookeeper | 2181 | Kafka coordinator |
| `kafka` | confluentinc/cp-kafka | 9092 | Event broker |
| `kafka-ui` | provectuslabs/kafka-ui | 8080 | Kafka management UI |
| `redis` | redis:7-alpine | 6379 | BFF session store |
| `dbgate` | dbgate/dbgate | 3000 | DB management UI |

## Networks

- `aegis-network` — All services communicate over this internal network

## Volumes

- `postgres-identity-data`
- `postgres-wallet-data`
- `kafka-data`
- `zookeeper-data`
- `redis-data`

## Configuration Files

- `infra/docker-compose.yml` — Main compose file
- `infra/docker-compose.dev.yml` — Dev overlay (hot-reload volumes)

## Related

- [[01 - Services/Identity Service\|Identity Service]] → connects to `postgres-identity`
- [[01 - Services/Wallet Service\|Wallet Service]] → connects to `postgres-wallet`
- [[01 - Services/BFF Service\|BFF Service]] → connects to `redis`
