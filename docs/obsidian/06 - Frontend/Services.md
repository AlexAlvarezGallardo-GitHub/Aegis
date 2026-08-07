---
type: frontend
tags: [angular, services, http]
status: implemented
---

# Frontend Services

## Core Services

| Service | Location | Description |
|---------|----------|-------------|
| `AuthService` | `features/auth` | `login`, `mock-login`, `logout`, `refresh`, `me` against `/api/bff/auth`; `BehaviorSubject` authenticated state |
| `RegistrationService` | `features/registration` | Registration via `POST /api/v1/users/register` (direct to Identity, pre-auth endpoint) |
| `WalletService` | `features/wallet` | Wallet CRUD, deposits, balance adjustment, status change against `/api/bff/wallets` |
| `ToastService` | `shared/services` | Signals-based toast notifications |
| `ThemeService` | `shared/services` | Light/dark/system theme, persisted in `localStorage` |
| `KeyboardShortcutsService` | `shared/services` | Global keyboard shortcuts |
| `ConfirmationService` | `shared/services` | Confirmation dialogs |
| `CommandPaletteService` | `shared/services` | Command palette state |
| `IconRegistryService` | `shared/icons` | SVG icon registration |

## Interceptors

Applied in order:

| Interceptor | Description |
|-------------|-------------|
| `HttpTimeoutInterceptor` | HTTP timeout (15s) |
| `HttpAuthInterceptor` | Auth header (stub/passthrough in dev) |
| `HttpErrorInterceptor` | Maps HTTP error codes to toast; 401 → logout + redirect to `/login` with `returnUrl` |

## Guards

| Guard | Description |
|-------|-------------|
| `AuthGuard` | Protects authenticated routes, redirects to `/login` with `returnUrl`; auto-auth when `enableMockLogin` |

## Models & Utils

- **Models**: `auth`, `registration`, `wallet`, `error`
- **Utils**: `validation` (email, `passwordStrength`, `currencyCode`)

## Proxy Configuration

- `proxy.conf.json` — Dev proxy: `/api/bff/*` → BFF `:8082`; `/api/*` → Identity `:8081`
- `proxy.conf.docker.json` — Docker proxy variant
