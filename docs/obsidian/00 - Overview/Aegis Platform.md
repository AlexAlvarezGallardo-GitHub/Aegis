---
type: overview
tags: [aegis, platform, root]
status: implemented
---

# Aegis Platform

**Enterprise-grade digital payment platform** built with microservices, event-driven architecture, and hexagonal design.

```mermaid
graph TB
    subgraph Frontend
        BFF["BFF Service<br/>:8082"]
    end
    subgraph Services
        Identity["Identity Service<br/>:8081"]
        Wallet["Wallet Service<br/>:8083"]
        Fraud["Fraud Service<br/>:8089"]
        Report["Reporting Service<br/>:8087"]
        Audit["Audit Service<br/>:8088"]
    end
    subgraph Infrastructure
        Kafka["Apache Kafka"]
        PG1[("PostgreSQL<br/>identity")]
        PG2[("PostgreSQL<br/>wallet")]
        PG3[("PostgreSQL<br/>reporting")]
        PG4[("PostgreSQL<br/>audit")]
        PG5[("PostgreSQL<br/>fraud")]
    end
    User((User)) -->|HTTPS| BFF
    BFF --> Identity
    BFF --> Wallet
    Wallet -->|wallet.funds.deposited| Kafka
    Fraud -->|fraud.assessment.completed| Kafka
    Fraud -->|consumes payment.*| Kafka
    Kafka --> Report
    Kafka --> Audit
    Identity --> PG1
    Wallet --> PG2
    Report --> PG3
    Audit --> PG4
    Fraud --> PG5
    style User fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style BFF fill:#bbf,stroke:#333,color:#000
    style Identity fill:#bbf,stroke:#333,color:#000
    style Wallet fill:#bbf,stroke:#333,color:#000
    style Report fill:#bbf,stroke:#333,color:#000
    style Audit fill:#bbf,stroke:#333,color:#000
    style Fraud fill:#bbf,stroke:#333,color:#000
    style Kafka fill:#fdb,stroke:#333,color:#000
```

## Deposit Flow

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service
    participant Wallet as Wallet Service
    participant DB as PostgreSQL
    participant Kafka as Apache Kafka
    participant Report as Reporting Service
    participant Audit as Audit Service

    User->>BFF: POST /api/bff/wallets/{id}/deposits
    BFF->>Wallet: POST /api/v1/wallets/{id}/deposits
    Wallet->>DB: Validate & update balance
    Wallet->>DB: Create ledger entry
    Wallet->>DB: Save outbox event
    Wallet-->>BFF: 201 DepositReceipt
    BFF-->>User: 201 DepositReceipt
    DB->>Kafka: Outbox relay: FundsDeposited
    Kafka->>Report: Consume FundsDeposited
    Kafka->>Audit: Consume FundsDeposited
    Report->>PG3: Upsert BalanceProjection
    Audit->>PG4: Insert AuditRecord
```

## Context (C4 Level 1)

```mermaid
flowchart TB
    User([User])
    User -->|HTTPS| BFF["BFF Service<br/>:8082"]
    BFF --> Identity["Identity Service<br/>:8081"]
    BFF --> Wallet["Wallet Service<br/>:8083"]
    BFF --> Fraud["Fraud Service<br/>:8089"]
    Identity --> PG1[("PostgreSQL<br/>aegis_identity")]
    Wallet --> PG2[("PostgreSQL<br/>aegis_wallet")]
    Wallet -->|wallet.funds.deposited| Kafka[[Apache Kafka]]
    Fraud -->|fraud.assessment.completed| Kafka
    Kafka --> Report["Reporting Service<br/>:8087"]
    Kafka --> Audit["Audit Service<br/>:8088"]
    Report --> PG3[("PostgreSQL<br/>aegis_reporting")]
    Audit --> PG4[("PostgreSQL<br/>aegis_audit")]
    Fraud --> PG5[("PostgreSQL<br/>aegis_fraud")]
    style User fill:#f9f,stroke:#333,stroke-width:2px,color:#000
    style BFF fill:#bbf,stroke:#333,color:#000
    style Identity fill:#bbf,stroke:#333,color:#000
    style Wallet fill:#bbf,stroke:#333,color:#000
    style Fraud fill:#bbf,stroke:#333,color:#000
    style Report fill:#bbf,stroke:#333,color:#000
    style Audit fill:#bbf,stroke:#333,color:#000
    style Kafka fill:#fdb,stroke:#333,color:#000
    style PG1 fill:#afa,stroke:#333,color:#000
    style PG2 fill:#afa,stroke:#333,color:#000
    style PG3 fill:#afa,stroke:#333,color:#000
    style PG4 fill:#afa,stroke:#333,color:#000
    style PG5 fill:#afa,stroke:#333,color:#000
```

## Services

| Service | Port | Tech | Status |
|---------|------|------|--------|
| [[01 - Services/BFF Service\|BFF Service]] | 8082 | Spring Boot + WebClient | ✅ |
| [[01 - Services/Identity Service\|Identity Service]] | 8081 | Spring Boot + JPA + Security | ✅ |
| [[01 - Services/Wallet Service\|Wallet Service]] | 8083 | Spring Boot + JPA | ✅ |
| [[01 - Services/Reporting Service\|Reporting Service]] | 8087 | Spring Boot + JPA + Kafka | ✅ |
| [[01 - Services/Audit Service\|Audit Service]] | 8088 | Spring Boot + JPA + Kafka | ✅ |
| [[01 - Services/Fraud Service\|Fraud Service]] | 8089 | Spring Boot + JPA + Kafka | ✅ |
| [[01 - Services/Common Module\|Common Module]] | — | Shared library | ✅ |
| [[01 - Services/Frontend\|Frontend]] | 4200 | Angular 22 | ✅ |

## Domain Models

| Model | Service | Type |
|-------|---------|------|
| [[02 - Domain Models/User\|User]] | Identity | Aggregate Root |
| [[02 - Domain Models/Email\|Email]] | Identity | Value Object |
| [[02 - Domain Models/UserId\|UserId]] | Identity | Value Object |
| [[02 - Domain Models/PasswordHash\|PasswordHash]] | Identity | Value Object |
| [[02 - Domain Models/UserStatus\|UserStatus]] | Identity | Enum |
| [[02 - Domain Models/Credentials\|Credentials]] | Identity | Value Object |
| [[02 - Domain Models/TokenPair\|TokenPair]] | Identity | Value Object |
| [[02 - Domain Models/Wallet\|Wallet]] | Wallet | Aggregate Root |
| [[02 - Domain Models/WalletId\|WalletId]] | Wallet | Value Object |
| [[02 - Domain Models/WalletStatus\|WalletStatus]] | Wallet | Enum |
| [[02 - Domain Models/LedgerEntry\|LedgerEntry]] | Wallet | Value Object |
| [[02 - Domain Models/LedgerEntryType\|LedgerEntryType]] | Wallet | Enum |

## Domain Events

| Event | Producer | Topic |
|-------|----------|-------|
| [[03 - Domain Events/UserRegistered\|UserRegistered]] | Identity | `aegis.identity.user-registered` |
| [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] | Identity | `aegis.identity.user-authenticated` |
| [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] | Identity | `aegis.identity.user-account-locked` |
| [[03 - Domain Events/WalletCreated\|WalletCreated]] | Wallet | `aegis.wallet.wallet-created` |
| [[03 - Domain Events/WalletBalanceAdjusted\|WalletBalanceAdjusted]] | Wallet | `aegis.wallet.balance.adjusted` |
| [[03 - Domain Events/FundsDeposited\|FundsDeposited]] | Wallet | `wallet.funds.deposited` |
| [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] | Fraud | `fraud.assessment.completed` |

## Ports (Hexagonal Architecture)

**Inbound (Driving)**
- [[04 - Ports/inbound/RegisterUserUseCase\|RegisterUserUseCase]] → [[01 - Services/Identity Service|Identity Service]]
- [[04 - Ports/inbound/AuthenticateUserUseCase\|AuthenticateUserUseCase]] → [[01 - Services/Identity Service|Identity Service]]
- [[04 - Ports/inbound/CreateWalletUseCase\|CreateWalletUseCase]] → [[01 - Services/Wallet Service|Wallet Service]]
- [[04 - Ports/inbound/UpdateWalletUseCase\|UpdateWalletUseCase]] → [[01 - Services/Wallet Service|Wallet Service]]
- [[04 - Ports/inbound/DepositFundsUseCase\|DepositFundsUseCase]] → [[01 - Services/Wallet Service|Wallet Service]]
- [[04 - Ports/inbound/AssessFraudUseCase\|AssessFraudUseCase]] → [[01 - Services/Fraud Service|Fraud Service]]

**Outbound (Driven)**
- [[04 - Ports/outbound/UserRepository\|UserRepository]] → [[01 - Services/Identity Service|Identity Service]]
- [[04 - Ports/outbound/PasswordHasher\|PasswordHasher]] → [[01 - Services/Identity Service|Identity Service]]
- [[04 - Ports/outbound/TokenProvider\|TokenProvider]] → [[01 - Services/Identity Service|Identity Service]]
- [[04 - Ports/outbound/EventPublisher\|EventPublisher]] → [[01 - Services/Identity Service|Identity]] / [[01 - Services/Wallet Service|Wallet]]
- [[04 - Ports/outbound/WalletRepository\|WalletRepository]] → [[01 - Services/Wallet Service|Wallet Service]]

## Infrastructure

- [[05 - Infrastructure/GitOps\|GitOps]] — Declarative deployment workflow
- [[05 - Infrastructure/Argo CD\|Argo CD]] — GitOps engine, app-of-apps auto-discovery, environment promotion
- [[05 - Infrastructure/Helm Charts\|Helm Charts]] — Charts and environment overlays
- [[05 - Infrastructure/Docker Services\|Docker Services]] — Local dev stack (PostgreSQL, Kafka, Redis) via Docker Compose
- [[05 - Infrastructure/Kafka Topics\|Kafka Topics]] — Event catalog
- [[05 - Infrastructure/Database Schema\|Database Schema]] — Database schemas
- [[05 - Infrastructure/Flyway Migrations\|Flyway Migrations]] — DB versioning

> **Deployment status (dev):** all services (`identity`, `wallet`, `bff`, `frontend`) and infrastructure (`database`, `kafka`, `redis`) run in a minikube cluster, managed by Argo CD through the `app-of-apps-dev` Application. See [[05 - Infrastructure/Argo CD\|Argo CD]].

## Specifications

- [[07 - Specs/UC-001 User Registration\|UC-001 User Registration]]
- [[07 - Specs/UC-002 User Authentication\|UC-002 User Authentication]]
- [[07 - Specs/UC-003 Create Wallet\|UC-003 Create Wallet]]
- [[07 - Specs/UC-004 Deposit Funds\|UC-004 Deposit Funds]]
- [[07 - Specs/UC-008 Fraud Detection\|UC-008 Fraud Detection]]
- [[07 - Specs/UC-010 BFF\|UC-010 BFF]]

## Architecture Decisions

See [[00 - Overview/Architecture Decisions|Architecture Decisions]] for ADR index.
