---
type: frontend
tags: [angular, routes, navigation]
status: implemented
---

# Frontend Routes

| Path | Component | Auth Guard | Description |
|------|-----------|------------|-------------|
| `/dashboard` | `DashboardComponent` | ✅ | KPI cards, charts, activity |
| `/wallets` | `WalletListComponent` | ✅ | Wallet CRUD table |
| `/wallets/:id` | `WalletDetailComponent` | ✅ | Wallet detail view |
| `/login` | `AuthComponent` | ❌ | Login form |
| `/register` | `RegistrationComponent` | ❌ | Registration form |
| `/payments` | `PagePlaceholderComponent` | ✅ | Placeholder |
| `/fraud` | `PagePlaceholderComponent` | ✅ | Placeholder |
| `/settings` | `PagePlaceholderComponent` | ✅ | Placeholder |
| `**` | Redirect to `/login` | — | Wildcard fallback |
