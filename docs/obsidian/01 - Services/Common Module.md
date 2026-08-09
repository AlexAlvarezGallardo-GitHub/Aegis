---
type: service
service: aegis-common
layer: all
tags: [java, shared, util]
status: implemented
---

# Common Module

**Purpose**: Shared library used by all backend services — UUID v7 generation, base exceptions, and web advice utilities. No port, no database.

```mermaid
graph TB
    subgraph "aegis-common"
        direction TB
        Domain["domain/exception<br/>AegisException (base)"]
        Util["util/<br/>UuidV7Generator"]
        Web["web/advice<br/>AbstractExceptionHandler"]
    end
    Identity["Identity Service"] -->|depends on| Common["aegis-common"]
    Wallet["Wallet Service"] -->|depends on| Common
    BFF["BFF Service"] -->|depends on| Common
    Fraud["Fraud Service"] -->|depends on| Common
    Audit["Audit Service"] -->|depends on| Common
    Reporting["Reporting Service"] -->|depends on| Common
    style Common fill:#bbf,stroke:#333,color:#000
    style Identity fill:#bbf,stroke:#333,color:#000
    style Wallet fill:#bbf,stroke:#333,color:#000
    style BFF fill:#bbf,stroke:#333,color:#000
    style Fraud fill:#bbf,stroke:#333,color:#000
    style Audit fill:#bbf,stroke:#333,color:#000
    style Reporting fill:#bbf,stroke:#333,color:#000
    style Domain fill:#fdb,stroke:#333,color:#000
    style Util fill:#fdb,stroke:#333,color:#000
    style Web fill:#fdb,stroke:#333,color:#000
```

## Contents

### Exceptions
- `AegisException` — Base class for all domain exceptions

### Utilities
- `UuidV7Generator` — Time-ordered UUID v7 generation (DB-friendly)

### Web Advice
- `AbstractExceptionHandler` — Base REST exception handler

## Dependencies

- **Depended by**: [[01 - Services/Identity Service\|Identity Service]], [[01 - Services/Wallet Service\|Wallet Service]], [[01 - Services/BFF Service\|BFF Service]], [[01 - Services/Fraud Service\|Fraud Service]], [[01 - Services/Audit Service\|Audit Service]], [[01 - Services/Reporting Service\|Reporting Service]]
