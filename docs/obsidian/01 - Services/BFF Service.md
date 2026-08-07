---
type: service
service: aegis-bff-service
layer: all
tags: [java, spring, webclient, proxy]
status: implemented
port: 8082
---

# BFF Service

**Purpose**: Backend-for-Frontend — single entry point for the Angular SPA. Handles login/logout/refresh with an HTTP session (Redis, HttpOnly cookies) and proxies authenticated requests to the Wallet Service. Mock-login is available only on the `dev` profile.

```mermaid
graph TB
    subgraph Browser
        Angular[Angular SPA]
    end
    subgraph "BFF Service :8082"
        AuthCtrl[BffAuthController<br/>MockAuthController]
        WalletCtrl[BffWalletController]
        JwtFilter[SessionJwtAuthenticationFilter]
        JwtStore[SessionJwtStore]
    end
    subgraph Backend
        Identity[Identity Service :8081]
        Wallet[Wallet Service :8083]
    end
    subgraph External
        Redis[(Redis Session Store)]
    end
    Angular -->|POST /api/bff/auth/*| AuthCtrl
    Angular -->|GET/POST/PATCH /api/bff/wallets/*| WalletCtrl
    JwtFilter -->|check session| JwtStore
    JwtStore --> Redis
    AuthCtrl -->|proxies| Identity
    WalletCtrl -->|proxies| Wallet
    style Angular fill:#bbf,stroke:#333,color:#000
    style Identity fill:#bbf,stroke:#333,color:#000
    style Wallet fill:#bbf,stroke:#333,color:#000
    style AuthCtrl fill:#bbf,stroke:#333,color:#000
    style WalletCtrl fill:#bbf,stroke:#333,color:#000
    style JwtFilter fill:#fdb,stroke:#333,color:#000
    style JwtStore fill:#fdb,stroke:#333,color:#000
    style Redis fill:#afa,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Client as Angular
    participant BFF as BFF WalletController
    participant Wallet as Wallet Service
    participant DB as PostgreSQL

    Client->>BFF: POST /api/bff/wallets/{walletId}/deposits
    BFF->>BFF: extract userId from session JWT
    BFF->>Wallet: POST /api/v1/wallets/{walletId}/deposits (X-User-Id, X-Correlation-Id)
    Wallet->>DB: process deposit
    Wallet-->>BFF: 201 DepositReceipt
    BFF-->>Client: 201 DepositReceipt (passthrough)
```

## Structure

### Core
- `BffApplication.java` — Main class
- `BffAuthController.java` — Login/refresh/logout/me endpoints
- `MockAuthController.java` — Mock-login endpoint (dev profile only)
- `BffWalletController.java` — Wallet proxy endpoints
- `BffService.java` — WebClient-based proxy to backend services
- `MockLoginService.java` — Creates a mock session (dev profile)
- `SessionJwtStore.java` — JWT storage in Redis-backed HttpSession

### Domain Ports
- `IdentityClient` — Identity Service HTTP client
- `WalletClient` — Wallet Service HTTP client
- `TokenStore` — Session-bound JWT storage
- `TokenValidator` — Validates session JWT
- `JwtSigningKey` — Shared signing key provider

### Security
- `SecurityConfig.java` — CSRF, session management, permit auth routes
- `SessionJwtAuthenticationFilter.java` — Attach JWT to proxied requests

### Config
- `BffProperties.java` — Backend service URLs
- `application.yml` — Port 8082, Redis, identity/wallet URLs
- `application-dev.yml` — Dev overrides (enables mock-login)

## API Endpoints

| Method | Path | Proxies To / Action |
|--------|------|---------------------|
| POST | `/api/bff/auth/login` | Identity: `POST /api/v1/auth/login` |
| POST | `/api/bff/auth/refresh` | Identity: `POST /api/v1/auth/refresh` |
| POST | `/api/bff/auth/logout` | Session invalidation (204) |
| GET | `/api/bff/auth/me` | Session user info |
| POST | `/api/bff/auth/mock-login` | Dev-only mock session |
| GET | `/api/bff/wallets` | Wallet: `GET /api/v1/wallets` |
| POST | `/api/bff/wallets` | Wallet: `POST /api/v1/wallets` |
| GET | `/api/bff/wallets/{walletId}` | Wallet: `GET /api/v1/wallets/{walletId}` |
| POST | `/api/bff/wallets/{walletId}/deposits` | Wallet: `POST /api/v1/wallets/{walletId}/deposits` |
| PATCH | `/api/bff/wallets/{walletId}/balance` | Wallet: `PATCH /api/v1/wallets/{walletId}/balance` |
| PATCH | `/api/bff/wallets/{walletId}/status` | Wallet: `PATCH /api/v1/wallets/{walletId}/status` |

## Dependencies

- **Depends on**: [[01 - Services/Identity Service\|Identity Service]], [[01 - Services/Wallet Service\|Wallet Service]], Redis
- **Depended by**: [[01 - Services/Frontend\|Frontend]] (all API calls go through BFF)

## Key Design Decisions

| Decision | Choice |
|----------|--------|
| Session store | Redis (distributed) |
| HTTP client | WebClient (reactive) |
| CSRF | Enabled |
| Cookie | HttpOnly, SameSite=Strict |
| Kafka | BFF neither produces nor consumes events |
