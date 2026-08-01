---
type: service
service: aegis-frontend
layer: all
tags: [angular, typescript, material, spa]
status: implemented
port: 4200
---

# Frontend

**Purpose**: Angular 22 SPA with Material Design — the user-facing interface for the Aegis platform.

```mermaid
graph TB
    subgraph "Angular 22 SPA"
        direction TB
        Features["Features<br/>Dashboard, Wallet, Auth, Registration"]
        Shared["Shared<br/>Layout, DataDisplay, Guards, Interceptors"]
        Core["Core<br/>ThemeService, AuthGuard, HTTP Interceptors"]
        Features --> Shared --> Core
    end
    Browser["Browser"] -->|HTTP| Angular["Angular SPA :4200"]
    Angular -->|/api/* via proxy| BFF["BFF Service :8082"]
    style Angular fill:#bbf,stroke:#333,color:#000
    style Features fill:#fdb,stroke:#333,color:#000
    style Shared fill:#fdb,stroke:#333,color:#000
    style Core fill:#fdb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Browser)
    participant Angular as Angular SPA
    participant Proxy as proxy.conf.json
    participant BFF as BFF Service
    participant Identity as Identity Service
    participant Wallet as Wallet Service

    User->>Angular: Navigate to /wallets
    Angular->>Angular: AuthGuard checks session
    Angular->>Proxy: GET /api/bff/wallets
    Proxy->>BFF: forward to :8082
    BFF->>BFF: extract JWT from session
    BFF->>Wallet: GET /api/v1/wallets (X-User-Id)
    Wallet-->>BFF: wallet list
    BFF-->>Angular: 200 wallets
    Angular-->>User: render WalletListComponent
```

## Tech Stack

| Technology | Version |
|------------|---------|
| Angular | 22 |
| Angular Material | 22 |
| TypeScript | 6 |
| Reactive Forms | — |
| Chart.js / ng2-charts | Dashboard |
| Design Tokens | Custom SCSS |

## Architecture

### Features
- **Dashboard** — KPI cards, charts, activity feed, system health
- **Wallet List** — Stripe-style table with status badges, CRUD actions
- **Wallet Detail** — Overview, balance cards, transaction history, timeline, analytics
- **Auth** — Login, registration, profile
- **Layout** — Sidebar navigation, header, theme toggle

### Core Modules
- `AppShellComponent` — Layout wrapper with sidebar + header
- `ThemeService` — Dark/light mode with design tokens
- `AuthGuard` — Route protection
- HTTP interceptors — Auth token injection + error handling

### Shared Components (UX Foundation)
- `StatCardComponent` — KPI metric cards
- `StatusChipComponent` — Colored status badges
- `LoadingSkeletonComponent` — Skeleton loaders
- `EmptyStateComponent` — Empty state display
- `AegisIconComponent` — Custom SVG icons

### Design System
- Gold (#D4AF37) brand palette
- Design tokens: colors, typography, spacing, radius, shadows
- Dark/light theme support
- WCAG 2.1 AA compliant

## Routes

| Path | Component | Auth |
|------|-----------|------|
| `/dashboard` | DashboardComponent | ✅ |
| `/wallets` | WalletListComponent | ✅ |
| `/wallets/:id` | WalletDetailComponent | ✅ |
| `/login` | AuthComponent | ❌ |
| `/register` | RegistrationComponent | ❌ |

## Dependencies

- **Depends on**: [[01 - Services/BFF Service\|BFF Service]] (all API calls)
- **Proxy**: `proxy.conf.json` routes `/api/*` → BFF `:8082`
