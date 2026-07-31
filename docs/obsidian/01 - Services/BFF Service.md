---
type: service
service: aegis-bff-service
layer: all
tags: [java, spring, webclient, proxy]
status: implemented
port: 8082
---

# BFF Service

**Purpose**: Backend-for-Frontend — single entry point for the Angular SPA. Proxies auth and wallet requests, manages session-based JWT storage, and hides internal service topology from the frontend.

```mermaid
graph TB
    subgraph Browser
        Angular[Angular SPA]
    end
    subgraph "BFF Service :8082"
        AuthCtrl[BffAuthController]
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
```

```mermaid
sequenceDiagram
    participant Client as Angular
    participant BFF as BFF WalletController
    participant Wallet as Wallet Service
    participant DB as PostgreSQL

    Client->>BFF: POST /api/bff/wallets/{id}/deposits
    BFF->>BFF: extract userId from session JWT
    BFF->>Wallet: POST /api/v1/wallets/{id}/deposits (X-User-Id, X-Correlation-Id)
    Wallet->>DB: process deposit
    Wallet-->>BFF: 201 DepositReceipt
    BFF-->>Client: 201 DepositReceipt (passthrough)
```

## Structure

### Core
- `BffApplication.java` — Main class
- `BffAuthController.java` — Login/logout/me/refresh endpoints
- `BffWalletController.java` — Wallet proxy endpoints
- `BffService.java` — WebClient-based proxy to backend services
- `SessionJwtStore.java` — JWT storage in Redis-backed HttpSession

### Security
- `SecurityConfig.java` — CSRF, session management, permit auth routes
- `SessionJwtAuthenticationFilter.java` — Attach JWT to proxied requests

### Config
- `BffProperties.java` — Backend service URLs
- `application.yml` — Port 8082, Redis, identity/wallet URLs
- `application-dev.yml` — Dev overrides

## API Endpoints

| Method | Path | Proxies To |
|--------|------|------------|
| POST | `/api/bff/auth/login` | Identity: `POST /api/v1/auth/login` |
| POST | `/api/bff/auth/logout` | Session invalidation |
| GET | `/api/bff/auth/me` | Session user info |
| POST | `/api/bff/auth/refresh` | Identity: `POST /api/v1/auth/refresh` |
| GET | `/api/bff/wallets` | Wallet: `GET /api/v1/wallets` |
| POST | `/api/bff/wallets` | Wallet: `POST /api/v1/wallets` |
| POST | `/api/bff/wallets/{id}/deposits` | Wallet: `POST /api/v1/wallets/{id}/deposits` |
| PATCH | `/api/bff/wallets/{id}/balance` | Wallet: `PATCH /api/v1/wallets/{id}/balance` |
| PATCH | `/api/bff/wallets/{id}/status` | Wallet: `PATCH /api/v1/wallets/{id}/status` |

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
