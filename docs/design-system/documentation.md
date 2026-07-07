# Aegis Design System Documentation

**Version:** 1.0.0
**Last Updated:** 2026-07-07
**Framework:** Angular 22 + Angular Material 22 (M3)

---

## 1. Overview

The Aegis Design System provides a consistent visual language and component library for the Aegis payment platform. It is built on design tokens, Angular Material M3 theming, and shared reusable components.

### Architecture

```
src/
├── styles/
│   ├── tokens/          # Design tokens (colors, typography, spacing, etc.)
│   ├── themes/          # Light/dark Material theme overrides
│   └── mixins/          # SCSS mixins for common patterns
├── app/
│   ├── shared/
│   │   ├── forms/       # Shared form components
│   │   ├── data-display/# Shared data display components
│   │   ├── layout/      # App shell, sidebar, header
│   │   ├── icons/       # Custom SVG icons and registry
│   │   ├── services/    # ThemeService
│   │   └── utils/       # Validation utilities
│   └── features/        # Feature components
└── assets/
    └── icons/           # Custom SVG icon files
```

---

## 2. Design Tokens

All tokens are defined as CSS custom properties and SCSS maps in `src/styles/tokens/`.

### 2.1 Colors

#### Gold Palette (Primary)

| Token | Value | Preview |
|-------|-------|---------|
| `--aegis-gold-50` | `#FAF5E8` | 🟡 |
| `--aegis-gold-500` | `#C89B3C` | 🟡 |
| `--aegis-gold-900` | `#362B00` | 🟤 |

#### Neutral Scale

| Token | Value | Usage |
|-------|-------|-------|
| `--aegis-neutral-50` | `#F8FAFC` | Light backgrounds, text on dark |
| `--aegis-neutral-500` | `#64748B` | Muted text |
| `--aegis-neutral-900` | `#0B1220` | Dark backgrounds, text on light |

#### Semantic Colors

| Token | Dark | Light |
|-------|------|-------|
| `--aegis-color-success` | `#10B981` | `#10B981` |
| `--aegis-color-warning` | `#F59E0B` | `#F59E0B` |
| `--aegis-color-error` | `#EF4444` | `#EF4444` |
| `--aegis-color-info` | `#3B82F6` | `#3B82F6` |

#### Usage

```scss
// CSS custom property
color: var(--aegis-color-primary);

// SCSS map
@use 'styles/tokens' as tokens;
color: map.get(map.get(tokens.$aegis-colors, gold), 500);
```

### 2.2 Typography

| Token | Value | Usage |
|-------|-------|-------|
| `--aegis-font-brand` | `Inter, sans-serif` | Primary font |
| `--aegis-text-xs` | `0.6875rem` (11px) | Badges, captions |
| `--aegis-text-sm` | `0.75rem` (12px) | Labels, hints |
| `--aegis-text-base` | `0.875rem` (14px) | Body text |
| `--aegis-text-lg` | `1rem` (16px) | Buttons, inputs |
| `--aegis-text-xl` | `1.125rem` (18px) | Subtitles |
| `--aegis-text-3xl` | `1.5rem` (24px) | Card titles |
| `--aegis-text-4xl` | `1.875rem` (30px) | KPI values |
| `--aegis-text-5xl` | `2.25rem` (36px) | Hero text |

### 2.3 Spacing

4px base unit scale: `0, 2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56, 64, 80, 96, 112, 128`

```scss
padding: var(--aegis-space-4);  // 16px
margin: var(--aegis-space-6);   // 24px
gap: var(--aegis-space-2);      // 8px
```

### 2.4 Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `--aegis-radius-sm` | `4px` | Small inputs, chips |
| `--aegis-radius-md` | `6px` | Buttons, cards |
| `--aegis-radius-lg` | `8px` | Large cards |
| `--aegis-radius-xl` | `12px` | Modals |
| `--aegis-radius-full` | `9999px` | Avatars, pills |

### 2.5 Shadows

| Token | Value | Usage |
|-------|-------|-------|
| `--aegis-shadow-sm` | `0 1px 2px rgba(0,0,0,0.3)` | Subtle elevation |
| `--aegis-shadow-md` | `0 4px 12px rgba(0,0,0,0.25)` | Cards |
| `--aegis-shadow-lg` | `0 8px 24px rgba(0,0,0,0.3)` | Hover states |
| `--aegis-shadow-xl` | `0 12px 40px rgba(0,0,0,0.4)` | Modals, popovers |

### 2.6 Transitions

| Token | Value |
|-------|-------|
| `--aegis-duration-fast` | `150ms` |
| `--aegis-duration-normal` | `200ms` |
| `--aegis-duration-slow` | `300ms` |
| `--aegis-ease-out` | `cubic-bezier(0, 0, 0.2, 1)` |
| `--aegis-ease-in-out` | `cubic-bezier(0.4, 0, 0.2, 1)` |

---

## 3. Theme System

### 3.1 Architecture

- **`theme.scss`** — Applies Material theme overrides via `html[data-theme]` selector
- **`styles/themes/_dark.scss`** — Dark theme `--mat-sys-*` values derived from `$aegis-colors` map
- **`styles/themes/_light.scss`** — Light theme `--mat-sys-*` values derived from `$aegis-colors` map

### 3.2 ThemeService

```typescript
const theme = inject(ThemeService);
theme.isDark();        // computed signal: true/false
theme.getPreference(); // 'light' | 'dark' | 'system'
theme.setPreference('dark');
theme.toggle();
```

### 3.3 Theme Toggle Component

```html
<app-theme-toggle></app-theme-toggle>
```

---

## 4. Shared Components

### 4.1 Form Components

#### `LoadingButtonComponent` (`<app-aegis-loading-button>`)

```html
<app-aegis-loading-button
  label="Submit"
  [loading]="isLoading"
  [disabled]="form.invalid"
  [fullWidth]="true"
  variant="primary"
  type="submit"
/>
```

**Variants:** `primary`, `secondary`, `outline`, `ghost`, `danger`

#### `PasswordInputComponent` (`<app-aegis-password-input>`)

```html
<app-aegis-password-input
  label="Password"
  placeholder="Enter password"
  [fullWidth]="true"
  formControlName="password"
/>
```

Implements `ControlValueAccessor` for reactive forms.

#### `FormFieldErrorComponent` (`<app-aegis-form-field-error>`)

```html
<mat-form-field>
  <mat-label>Email</mat-label>
  <input matInput formControlName="email">
  <app-aegis-form-field-error [form]="form" controlName="email" [labels]="labels" />
</mat-form-field>
```

Supports: `required`, `email`, `minlength`, `maxlength`, `min`, `max`, `pattern`.

### 4.2 Data Display Components

#### `StatusChipComponent` (`<app-status-chip>`)

```html
<app-status-chip
  [label]="wallet.status"
  [variant]="'success'"
  size="md"
/>
```

**Variants:** `neutral`, `success`, `warning`, `error`, `info`
**Sizes:** `sm`, `md`, `lg`

#### `EmptyStateComponent` (`<app-empty-state>`)

```html
<app-empty-state
  icon="account_balance_wallet"
  title="No wallets yet"
  description="Create your first wallet."
  actionLabel="Create Wallet"
  actionRoute="/wallets/new"
/>
```

#### `StatCardComponent` (`<app-stat-card>`)

```html
<app-stat-card
  label="Total Balance"
  value="$12,345.67"
  icon="account_balance_wallet"
  trend="up"
  trendValue="+12.5%"
/>
```

#### `LoadingSkeletonComponent` (`<app-loading-skeleton>`)

```html
<app-loading-skeleton variant="card" [count]="3" />
```

**Variants:** `text`, `circle`, `rect`, `card`

### 4.3 Layout Components

#### `AppShellComponent`

Responsive sidenav layout. Authenticated routes render inside shell; public routes (login/register) render full-page.

#### `SidebarComponent`

Collapsible navigation with section grouping, active route indicator, localStorage persistence.

#### `HeaderComponent`

Top bar with branding, page title, theme toggle, notifications, user menu.

### 4.4 Icon System

#### `AegisIconComponent` (`<app-aegis-icon>`)

```html
<app-aegis-icon name="aegis-wallet" size="lg" />
<app-aegis-icon name="home" size="md" />
```

**Sizes:** `sm`, `md`, `lg`, `xl`
Custom icons: `aegis-shield`, `aegis-wallet`, `aegis-payment`, `aegis-fraud-alert`, `aegis-transaction`, `aegis-empty-data`, `aegis-error-state`, `aegis-success-state`, `aegis-maintenance`.

---

## 5. Accessibility

- All form fields use `mat-label` for automatic association
- Password toggles have `aria-label`
- Decorative icons use `aria-hidden="true"`
- Status chips use `role="status"` and `aria-label`
- Cards use `role="region"` and `aria-labelledby`
- Loading spinners and skeletons have `aria-label="Loading"`
- Color-independent error states (icons + text)

---

## 6. SCSS Mixins

```scss
@use 'styles/mixins/forms' as forms;

.card {
  @include forms.aegis-form-card;
}

.button {
  @include forms.aegis-submit-button;
}
```

Available mixins: `aegis-form-card`, `aegis-submit-button`, `aegis-full-width`, `aegis-loading-container`, `aegis-empty-state`.

---

## 7. Validation Utilities

```typescript
import { emailValidator, passwordStrengthValidator, currencyCodeValidator, markFormGroupTouched } from './shared/utils/validation.utils';

// Custom validators
email: ['', [Validators.required, emailValidator()]],
password: ['', [Validators.required, passwordStrengthValidator()]],
currency: ['', [Validators.required, currencyCodeValidator()]],

// Mark all fields as touched on submit
markFormGroupTouched(this.form);
```
