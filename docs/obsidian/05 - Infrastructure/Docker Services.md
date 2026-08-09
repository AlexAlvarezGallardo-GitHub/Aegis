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
        Reporting["aegis-reporting :8087"]
        Audit["aegis-audit :8088"]
        Fraud["aegis-fraud :8089"]
        Frontend["aegis-frontend :4200"]
    end
    subgraph "Admin"
        DbGate["dbgate :3000"]
    end
    Identity --> PG_I
    Wallet --> PG_W
    Reporting --> PG_R
    Audit --> PG_A
    Fraud --> PG_F
    BFF --> Redis
    Frontend --> BFF
    Kafka --> ZK
    KUI --> Kafka
    Kafka --> Reporting
    Kafka --> Audit
    Kafka --> Fraud
    DbGate --> PG_I & PG_W & PG_R & PG_A & PG_F & Redis
    style PG_I fill:#afa,stroke:#333,color:#000
    style PG_W fill:#afa,stroke:#333,color:#000
    style PG_R fill:#afa,stroke:#333,color:#000
    style PG_A fill:#afa,stroke:#333,color:#000
    style PG_F fill:#afa,stroke:#333,color:#000
    style Redis fill:#fdb,stroke:#333,color:#000
    style Identity fill:#bbf,stroke:#333,color:#000
    style Wallet fill:#bbf,stroke:#333,color:#000
    style BFF fill:#bbf,stroke:#333,color:#000
    style Reporting fill:#bbf,stroke:#333,color:#000
    style Audit fill:#bbf,stroke:#333,color:#000
    style Fraud fill:#bbf,stroke:#333,color:#000
    style Frontend fill:#bbf,stroke:#333,color:#000
```

## Infrastructure Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `postgres-identity` | postgres:16.4-alpine | 5432 | Identity DB |
| `postgres-wallet` | postgres:16.4-alpine | 5433 | Wallet DB |
| `postgres-reporting` | postgres:16.4-alpine | 5434 | Reporting DB |
| `postgres-audit` | postgres:16.4-alpine | 5435 | Audit DB |
| `postgres-fraud` | postgres:16.4-alpine | 5436 | Fraud DB |
| `zookeeper` | confluentinc/cp-zookeeper:7.5.0 | 2181 | Kafka coordinator |
| `kafka` | confluentinc/cp-kafka:7.5.0 | 9092 | Event broker |
| `kafka-ui` | provectuslabs/kafka-ui:latest | 8090 | Kafka management UI |
| `redis` | redis:7-alpine | 6379 | BFF session store |
| `dbgate` | dbgate/dbgate:6.2.0 | 3000 | DB management UI |

## Application Services

| Service | Dockerfile | Port | Database |
|---------|------------|------|----------|
| `aegis-identity` | `aegis-identity-service/Dockerfile` | 8081 | `aegis_identity` |
| `aegis-wallet` | `aegis-wallet-service/Dockerfile` | 8083 | `aegis_wallet` |
| `aegis-bff` | `aegis-bff-service/Dockerfile` | 8082 | — (Redis) |
| `aegis-reporting` | `aegis-reporting-service/Dockerfile` | 8087 | `aegis_reporting` |
| `aegis-audit` | `aegis-audit-service/Dockerfile` | 8088 | `aegis_audit` |
| `aegis-fraud` | `aegis-fraud-service/Dockerfile` | 8089 | `aegis_fraud` |
| `aegis-frontend` | `frontend/aegis-frontend/Dockerfile` | 4200→80 | — |

## Networks

- `aegis-network` — All services communicate over this internal network

## Volumes

- `postgres_data`
- `postgres_wallet_data`
- `postgres_reporting_data`
- `postgres_audit_data`
- `postgres_fraud_data`

## Configuration Files

- `infra/docker-compose.yml` — Main compose file
- `infra/docker-compose.dev.yml` — Dev overlay (hot-reload volumes)

## Related

- [[01 - Services/Identity Service\|Identity Service]] → connects to `postgres-identity`
- [[01 - Services/Wallet Service\|Wallet Service]] → connects to `postgres-wallet`
- [[01 - Services/BFF Service\|BFF Service]] → connects to `redis`
- [[01 - Services/Reporting Service\|Reporting Service]] → connects to `postgres-reporting`
- [[01 - Services/Audit Service\|Audit Service]] → connects to `postgres-audit`
- [[01 - Services/Fraud Service\|Fraud Service]] → connects to `postgres-fraud`
