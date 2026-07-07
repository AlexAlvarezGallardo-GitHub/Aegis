# Migration Guide — Visual Identity & Design System

**Version:** 1.0.0
**Date:** 2026-07-07
**Parent Epic:** [#27 Visual Identity & Design System Overhaul](https://github.com/AlexAlvarezGallardo-GitHub/Aegis/issues/27)

---

## Overview

This guide documents the changes made to the Aegis frontend during the Visual Identity & Design System Overhaul (epic #27, issues #28–#37) and provides instructions for migrating existing and future components.

---

## 1. What Changed

### 1.1 New Directory Structure

```
src/
├── styles/
│   ├── tokens/           # Design tokens (NEW)
│   │   ├── _colors.scss
│   │   ├── _typography.scss
│   │   ├── _spacing.scss
│   │   ├── _radius.scss
│   │   ├── _shadows.scss
│   │   ├── _transitions.scss
│   │   └── _index.scss
│   ├── themes/           # Theme modules (NEW)
│   │   ├── _dark.scss
│   │   ├── _light.scss
│   │   └── _index.scss
│   └── mixins/           # SCSS mixins (NEW)
│       └── _forms.scss
├── app/
│   └── shared/
│       ├── forms/        # Shared form components (NEW)
│       │   ├── loading-button/
│       │   ├── password-input/
│       │   └── form-field-error/
│       ├── data-display/ # Shared display components (NEW)
│       │   ├── status-chip/
│       │   ├── empty-state/
│       │   ├── stat-card/
│       │   └── loading-skeleton/
│       ├── layout/       # App shell, sidebar, header (NEW)
│       │   ├── app-shell/
│       │   ├── sidebar/
│       │   └── header/
│       ├── icons/        # Custom SVG icons (NEW)
│       ├── services/     # ThemeService (NEW)
│       └── utils/        # Validation utilities (NEW)
└── assets/
    └── icons/            # SVG icon files (NEW)
```

### 1.2 Changed Files

| File | Change |
|------|--------|
| `styles.scss` | Refactored to use token mixins with `html[data-theme]` selector |
| `theme.scss` | `--mat-sys-*` values now derived from `$aegis-colors` SCSS map |
| `angular.json` | Added `stylePreprocessorOptions.includePaths: ["src"]` |
| `app.routes.ts` | Restructured with shell wrapper for authenticated routes |
| `auth.component.*` | Refactored to use shared form components |
| `registration.component.*` | Refactored to use shared form components |
| `wallet.component.*` | Refactored to use shared form + data display components |

---

## 2. Migration Steps for Existing Components

### 2.1 Replace Hardcoded Colors

**Before:**
```scss
color: #C89B3C;
background: #0B1220;
border: 1px solid #2A3448;
```

**After:**
```scss
color: var(--aegis-color-primary);
background: var(--aegis-color-bg);
border: 1px solid var(--aegis-color-border);
```

### 2.2 Replace Hardcoded Spacing

**Before:**
```scss
padding: 16px;
margin: 24px 0;
gap: 8px;
```

**After:**
```scss
padding: var(--aegis-space-4);
margin: var(--aegis-space-6) 0;
gap: var(--aegis-space-2);
```

### 2.3 Replace Hardcoded Typography

**Before:**
```scss
font-size: 1.5rem;
font-weight: 700;
line-height: 1.25;
```

**After:**
```scss
font-size: var(--aegis-text-3xl);
font-weight: var(--aegis-font-weight-bold);
line-height: var(--aegis-leading-tight);
```

### 2.4 Use Shared Form Components

**Before:**
```html
<mat-form-field>
  <mat-label>Password</mat-label>
  <input matInput [type]="hidePassword ? 'password' : 'text'">
  <button mat-icon-button matSuffix (click)="hidePassword = !hidePassword">
    <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
  </button>
</mat-form-field>

<button mat-raised-button [disabled]="isLoading || form.invalid">
  @if (isLoading) { <mat-spinner diameter="20"></mat-spinner> }
  @else { Submit }
</button>
```

**After:**
```html
<app-aegis-password-input label="Password" formControlName="password" />
<app-aegis-loading-button label="Submit" [loading]="isLoading" [disabled]="form.invalid" />
```

### 2.5 Use Shared Data Display Components

**Before:**
```html
<td>{{ wallet.status }}</td>
<p>No wallets yet. Create one above.</p>
```

**After:**
```html
<td>
  <app-status-chip [label]="wallet.status" [variant]="getStatusVariant(wallet.status)" />
</td>
<app-empty-state icon="account_balance_wallet" title="No wallets yet" description="Create your first wallet." />
```

### 2.6 Add Accessibility Attributes

**Before:**
```html
<mat-card>
  <mat-card-title>My Card</mat-card-title>
</mat-card>
```

**After:**
```html
<mat-card role="region" aria-labelledby="my-card-title">
  <mat-card-title id="my-card-title">My Card</mat-card-title>
</mat-card>
```

---

## 3. Creating New Components

### 3.1 Component Checklist

- [ ] Use `ChangeDetectionStrategy.OnPush`
- [ ] Use `inject()` instead of constructor injection
- [ ] Use `takeUntilDestroyed()` for subscription cleanup
- [ ] Consume design tokens (`var(--aegis-*)`)
- [ ] Add `aria-label` or `aria-labelledby` where appropriate
- [ ] Add `aria-hidden="true"` to decorative icons
- [ ] Import from `styles/tokens` for SCSS maps if needed

### 3.2 Routing

Authenticated routes should be children of the `AppShellComponent`:

```typescript
{
  path: '',
  loadComponent: () => import('./shared/layout/app-shell/app-shell.component')
    .then(m => m.AppShellComponent),
  children: [
    { path: 'my-feature', loadComponent: () => import('./features/my-feature/my-feature.component') },
  ],
},
```

Public routes (login, register) should be siblings:

```typescript
{
  path: 'login',
  loadComponent: () => import('./features/auth/auth.component'),
},
```

### 3.3 Form Validation

Use shared utilities:

```typescript
import { markFormGroupTouched } from '../../shared/utils/validation.utils';

onSubmit(): void {
  if (this.form.invalid) {
    markFormGroupTouched(this.form);
    this.snackBar.open('Please fix the form errors.', 'Close');
    return;
  }
  // ...
}
```

---

## 4. Theme Integration

### 4.1 Using ThemeService

```typescript
const theme = inject(ThemeService);
if (theme.isDark()) { /* dark mode logic */ }
theme.toggle();
```

### 4.2 Theme Toggle in Header

The `ThemeToggleComponent` is already included in the sidebar footer and header. No additional setup needed.

---

## 5. Bundle Size Notes

Current initial bundle: **523 kB** (23 kB over 500 kB warning threshold). This is within the 1 MB error budget. Future optimizations may include:
- Lazy-loading theme styles
- Tree-shaking unused Material modules
- Code splitting for large feature modules

---

## 6. Breaking Changes

- **None.** All changes are backward compatible. Legacy CSS custom property aliases are maintained in `styles.scss`.

---

## 7. Related Issues

| Issue | Title | PR |
|-------|-------|-----|
| #28 | Design Tokens | #38 |
| #29 | Component Library Audit | #39 |
| #30 | Theme Implementation | #40 |
| #31 | Layout & Navigation Redesign | #41 |
| #32 | Form & Input Components | #42 |
| #33 | Data Display Components | #43 |
| #34 | Icon System & Illustrations | #44 |
| #35 | Responsive & Accessibility | #45 |
| #36 | Documentation | — |
| #37 | Migration Guide | — |
