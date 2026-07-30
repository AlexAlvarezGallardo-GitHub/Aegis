---
type: frontend
tags: [angular, services, http]
status: implemented
---

# Frontend Services

## Core Services

| Service | Description |
|---------|-------------|
| `AuthService` | Login/logout, token management |
| `RegistrationService` | User registration HTTP |
| `WalletService` | Wallet CRUD HTTP (create, list, update, deactivate, reactivate) |
| `ThemeService` | Dark/light mode toggle |
| `FakeDataService` | Generates mock data for dashboard development |

## Interceptors

| Interceptor | Description |
|-------------|-------------|
| `AuthInterceptor` | Attaches auth token to requests |
| `ErrorInterceptor` | Global HTTP error handling + snackbar |

## Guards

| Guard | Description |
|-------|-------------|
| `AuthGuard` | Protects authenticated routes, redirects to login |

## Proxy Configuration

- `proxy.conf.json` — Dev proxy: `/api/*` → BFF `:8082`
- `proxy.conf.docker.json` — Docker proxy variant
