---
type: frontend
tags: [angular, components, ui]
status: implemented
---

# Frontend Components

Standalone Angular 22 components (21 `.component.ts`: 1 root + 6 feature + 14 shared).

## Root Component

| Component | Description |
|-----------|-------------|
| `AppComponent` | Root standalone component |

## Feature Components

| Component | Route | Description |
|-----------|-------|-------------|
| `AuthComponent` | `/login` | Login form (real + mock login in dev) |
| `RegistrationComponent` | `/register` | Registration form |
| `WalletComponent` | `/wallets` | Wallet CRUD + deposits |
| `WalletDetailComponent` | `/wallets/:walletId` | Wallet detail (tabs, deposit/withdraw modals) |
| `MoneyDialogComponent` | — | Deposit / withdraw modal |
| `TechnicalDetailsDialogComponent` | — | Technical details (wallet id, copy) |

## Shared Components (UX Foundation)

| Component | Location | Description |
|-----------|----------|-------------|
| `AppShellComponent` | `shared/layout` | Protected layout wrapper (sidebar + header) |
| `HeaderComponent` | `shared/layout` | App header |
| `SidebarComponent` | `shared/layout` | Navigation sidebar |
| `PagePlaceholderComponent` | `shared/layout` | Generic "under construction" placeholder |
| `CommandPaletteComponent` | `shared/components` | Cmd/Ctrl+K palette with fuzzy search |
| `ConfirmationDialogComponent` | `shared/components` | Confirm dialog (CDK Dialog) |
| `KeyboardShortcutCheatSheetComponent` | `shared/components` | Shortcut cheat sheet (`?` key) |
| `ToastContainerComponent` | `shared/components` | Toast container (signals) |

## Shared Data Display

| Component | Location | Description |
|-----------|----------|-------------|
| `StatCardComponent` | `shared/data-display` | KPI metric card with `CountUpDirective` |
| `StatusChipComponent` | `shared/data-display` | Colored status badge (variants) |
| `EmptyStateComponent` | `shared/data-display` | Empty state display |
| `LoadingSkeletonComponent` | `shared/data-display` | Skeleton loader |

## Shared Forms

| Component | Location | Description |
|-----------|----------|-------------|
| `FormFieldErrorComponent` | `shared/forms` | Validation error display |
| `LoadingButtonComponent` | `shared/forms` | Button with loading state |
| `PasswordInputComponent` | `shared/forms` | Password input (`ControlValueAccessor`) |

## Directives

| Directive | Location | Description |
|-----------|----------|-------------|
| `CountUpDirective` | `shared/directives` | Animated number count-up for `StatCardComponent` |
