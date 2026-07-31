---
type: infrastructure
tags: [kafka, messaging, events]
status: implemented
---

# Kafka Topics

All domain event topics in the Aegis platform.

```mermaid
graph LR
    subgraph Identity
        I_Reg[aegis.identity.user-registered]
        I_Auth[aegis.identity.user-authenticated]
        I_Lock[aegis.identity.user-account-locked]
    end
    subgraph Wallet
        W_Create[aegis.wallet.wallet-created]
        W_Balance[aegis.wallet.balance.adjusted]
        W_Deposit[wallet.funds.deposited]
    end
    subgraph Fraud
        F_Assessment[fraud.assessment.completed]
    end
    subgraph Consumers
        W_Deposit --> Report[Reporting group]
        W_Deposit --> Audit[Audit group]
        F_Assessment --> Audit
    end
    style Report fill:#bfb,stroke:#333
    style Audit fill:#bfb,stroke:#333
    style I_Reg fill:#fdb,stroke:#333
    style I_Auth fill:#fdb,stroke:#333
    style I_Lock fill:#fdb,stroke:#333
    style W_Create fill:#fdb,stroke:#333
    style W_Balance fill:#fdb,stroke:#333
    style W_Deposit fill:#fdb,stroke:#333
    style F_Assessment fill:#fdb,stroke:#333
```

## Identity Service Topics

| Topic | Event | Partitions | Retention |
|-------|-------|-----------|-----------|
| `aegis.identity.user-registered` | [[03 - Domain Events/UserRegistered\|UserRegistered]] | 3 | 7 days |
| `aegis.identity.user-authenticated` | [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] | 3 | 7 days |
| `aegis.identity.user-account-locked` | [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] | 1 | 30 days |

## Wallet Service Topics

| Topic | Event | Partitions | Retention |
|-------|-------|-----------|-----------|
| `aegis.wallet.wallet-created` | [[03 - Domain Events/WalletCreated\|WalletCreated]] | 3 | 7 days |
| `aegis.wallet.balance.adjusted` | [[03 - Domain Events/WalletBalanceAdjusted\|WalletBalanceAdjusted]] | 3 | 7 days |
| `wallet.funds.deposited` | [[03 - Domain Events/FundsDeposited\|FundsDeposited]] | 3 | 7 days |

## Fraud Service Topics

| Topic | Event | Partitions | Retention |
|-------|-------|-----------|-----------|
| `fraud.assessment.completed` | [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] | 3 | 30 days |

## Consumer Groups

| Group | Service | Topics Consumed |
|-------|---------|-----------------|
| `reporting-group` | [[01 - Services/Reporting Service\|Reporting Service]] | `wallet.funds.deposited` |
| `audit-group` | [[01 - Services/Audit Service\|Audit Service]] | `wallet.funds.deposited`, `fraud.assessment.completed` |
| `fraud-group` | [[01 - Services/Fraud Service\|Fraud Service]] | `payment.transfer.requested` |

## Naming Convention

```
aegis.<service>.<event-name>
```

## Delivery Semantics

- **Pattern**: Transactional outbox
- **Guarantee**: At-least-once delivery
- **Serialization**: JSON (avro planned for future)
