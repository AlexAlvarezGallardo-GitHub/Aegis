---
type: frontend
tags: [angular, routes, navigation]
status: implemented
---

# Frontend Routes

Lazy-loaded `loadComponent` routes (Angular 22, standalone-only). No `/dashboard` route exists.

| Path | Component | Auth Guard | Description |
|------|-----------|------------|-------------|
| `/login` | `AuthComponent` | ❌ | Login form (real + mock login in dev) |
| `/register` | `RegistrationComponent` | ❌ | Registration form (POST `/api/v1/users/register` direct to Identity) |
| `/` (root layout) | `AppShellComponent` | ✅ | Protected layout (sidebar + header) |
| `/wallets` | `WalletComponent` | ✅ | Wallet CRUD + deposits (default redirect after login) |
| `/wallets/:walletId` | `WalletDetailComponent` | ✅ | Wallet detail (tabs, deposit/withdraw, status) |
| `/payments` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/transactions` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/payouts` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/currencies` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/fraud` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/alerts` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/health` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/settings` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/users` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `/api-keys` | `PagePlaceholderComponent` | ✅ | Placeholder ("under construction") |
| `''` | Redirect to `/login` | — | Root redirect |
| `**` | Redirect to `/login` | — | Wildcard fallback |

## Details

- **Root layout** — `''` guards `AppShellComponent` with `AuthGuard`; inside it `''` redirects to `/wallets`.
- **AuthGuard** — Redirects to `/login` with `returnUrl` query param. In dev (`environment.enableMockLogin`) it auto-authenticates.
- **`/wallets/:walletId`** — Lazy-loaded `WalletDetailComponent` with a `title` route data entry.
