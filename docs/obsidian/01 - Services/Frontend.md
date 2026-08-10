---
type: service
service: aegis-frontend
layer: all
tags: [angular, typescript, material, spa]
status: implemented
port: 4200
---

# Frontend

**Purpose**: Angular 22 SPA with Material Design — the user-facing interface for the Aegis platform. It exposes three real features (auth, registration, wallet) and guards the rest as placeholder routes.

```mermaid
graph TB
    subgraph "Angular 22 SPA (standalone)"
        direction TB
        Features["Features<br/>Auth, Registration, Wallet"]
        Shared["Shared<br/>Layout, DataDisplay, Forms, Interceptors"]
        Core["Core<br/>AuthGuard, HTTP Interceptors, Signals"]
        Features --> Shared --> Core
    end
    Browser["Browser"] -->|HTTP| Angular["Angular SPA :4200"]
    Angular -->|/api/bff/* via proxy| BFF["BFF Service :8082"]
    style Angular fill:#bbf,stroke:#333,color:#000
    style BFF fill:#bbf,stroke:#333,color:#000
    style Features fill:#bbf,stroke:#333,color:#000
    style Shared fill:#fdb,stroke:#333,color:#000
    style Core fill:#fdb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Browser)
    participant Angular as Angular SPA
    participant Guard as AuthGuard
    participant Proxy as proxy.conf.json
    participant BFF as BFF Service
    participant Wallet as Wallet Service

    User->>Angular: Navigate to /wallets
    Angular->>Guard: canActivate checks session (GET /me)
    Guard->>Proxy: GET /api/bff/auth/me
    Proxy->>BFF: forward to :8082
    BFF-->>Angular: 200 session
    Angular->>Proxy: GET /api/bff/wallets
    Proxy->>BFF: forward to :8082
    BFF->>BFF: extract JWT from session
    BFF->>Wallet: GET /api/v1/wallets (X-User-Id)
    Wallet-->>BFF: wallet list
    BFF-->>Angular: 200 wallets
    Angular-->>User: render WalletComponent
```

## Tech Stack

| Technology | Version |
|------------|---------|
| Angular | 22 |
| Angular Material | 22 |
| TypeScript | 6 |
| State management | Angular Signals (no NgRx) |
| Build | Standalone components, lazy loading |

## Architecture

### Features (real)
- **Auth** — Login with email/password against the BFF session API
- **Registration** — New user sign-up
- **Wallet** — Wallet list, create, detail, balance adjust, deposits

### Placeholder routes
`payments`, `transactions`, `payouts`, `currencies`, `fraud`, `alerts`, `health`, `settings`, `users`, `api-keys` — all render the shared `PagePlaceholderComponent`. There is **no `/dashboard`** route.

### Shared Modules
- **Layout**: `AppShellComponent`, `HeaderComponent`, `SidebarComponent`, `PagePlaceholderComponent`
- **Data Display**: `StatCardComponent`, `StatusChipComponent`, `LoadingSkeletonComponent`, `EmptyStateComponent`
- **Forms**: `FormFieldErrorComponent`, `LoadingButtonComponent`, `PasswordInputComponent`
- **Components**: `ToastContainerComponent`, `ConfirmationDialogComponent`, `CommandPaletteComponent`, `KeyboardShortcutCheatSheetComponent`
- **Guards**: `AuthGuard` — protects `/wallets` and redirects to `/login` when the session is invalid
- **Interceptors**: `http-timeout` (15s timeout), `http-auth` (auth header stub), `http-error` (toast + redirect to `/login` on 401)
- **Services**: `ToastService`, `ConfirmationService`, `CommandPaletteService`, `KeyboardShortcutsService`

### Mock Login
`MockLoginService` is enabled via `environment.enableMockLogin` (dev only) and posts to `/api/bff/auth/mock-login`.

## Routes

| Path | Component | Auth |
|------|-----------|------|
| `/login` | `AuthComponent` | ❌ |
| `/register` | `RegistrationComponent` | ❌ |
| `/wallets` | `WalletComponent` | ✅ |
| `/payments`, `/transactions`, `/payouts`, `/currencies`, `/fraud`, `/alerts`, `/health`, `/settings`, `/users`, `/api-keys` | `PagePlaceholderComponent` | ✅ |

## Dependencies

- **Depends on**: [[01 - Services/BFF Service\|BFF Service]] (all API calls go through BFF)
- **Proxy**: `proxy.conf.json` routes `/api/bff/*` → BFF `:8082` (and `/api/*` → Identity `:8081`)
