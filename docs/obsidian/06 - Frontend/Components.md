---
type: frontend
tags: [angular, components, ui]
status: implemented
---

# Frontend Components

## Feature Components

| Component | Route | Description |
|-----------|-------|-------------|
| `DashboardComponent` | `/dashboard` | KPIs, charts, activity feed, system health |
| `WalletListComponent` | `/wallets` | Wallet table with CRUD actions |
| `WalletDetailComponent` | `/wallets/:id` | Wallet detail with timeline |
| `AuthComponent` | `/login` | Login form |
| `RegistrationComponent` | `/register` | Registration form |

## Shared / UX Foundation

| Component | Description |
|-----------|-------------|
| `AppShellComponent` | Layout wrapper (sidebar + header) |
| `StatCardComponent` | KPI metric card with trend |
| `StatusChipComponent` | Colored status badge |
| `LoadingSkeletonComponent` | Skeleton loader |
| `EmptyStateComponent` | Empty state display |
| `AegisIconComponent` | Custom SVG icons |
| `WalletEditDialogComponent` | Edit wallet name dialog |
| `DeactivateConfirmationDialogComponent` | Confirm deactivation |
| `PagePlaceholderComponent` | Generic placeholder for future routes |

## Shared Form Components

| Component | Description |
|-----------|-------------|
| `FormFieldComponent` | Wrapped Material form field |
| `FormErrorsComponent` | Validation error display |
