# Component Library Audit Report

**Date:** 2026-07-06
**Author:** Aegis Frontend Team
**Parent Epic:** [#27 Visual Identity & Design System Overhaul](https://github.com/AlexAlvarezGallardo-GitHub/Aegis/issues/27)
**Feature Issue:** [#29 Component Library Audit](https://github.com/AlexAlvarezGallardo-GitHub/Aegis/issues/29)
**Framework:** Angular 22.0.4 + Angular Material 22.0.0 (M3 theming)

---

## 1. Executive Summary

The Aegis frontend currently consists of **3 feature components** (Auth, Registration, Wallet) built with Angular standalone components and Angular Material. The codebase is small but well-structured with lazy-loaded routes, reactive forms, and a comprehensive design token system (completed in #28).

**Key findings:**
- 7 Angular Material modules in use across 3 components
- 7 duplicate template patterns identified for extraction into shared components
- 14 issues found (2 high, 5 medium, 7 low severity)
- No shared components, directives, or pipes exist yet
- No HTTP interceptors or auth guards configured
- All components use default change detection (no OnPush)

---

## 2. Component Inventory

### 2.1 Component Overview

| Component | Lines (TS) | Lines (HTML) | Lines (SCSS) | Test Lines | Material Modules |
|-----------|-----------|-------------|-------------|-----------|-----------------|
| `AppComponent` | 13 | 1 | 0 | 29 | None |
| `AuthComponent` | 121 | 55 | 75 | 140 | 8 |
| `RegistrationComponent` | 107 | 68 | 58 | 165 | 7 |
| `WalletComponent` | 115 | 80 | 84 | 153 | 8 |

**Total:** 356 lines of component logic, 204 lines of templates, 217 lines of styles, 487 lines of tests.

### 2.2 Angular Material Modules In Use

| Module | Auth | Registration | Wallet | Usage |
|--------|------|-------------|--------|-------|
| `MatCardModule` | Yes | Yes | Yes | Card containers for forms and data |
| `MatFormFieldModule` | Yes | Yes | Yes | Form field wrappers |
| `MatInputModule` | Yes | Yes | Yes | Text inputs |
| `MatButtonModule` | Yes | Yes | Yes | Raised, stroked, and icon buttons |
| `MatIconModule` | Yes | Yes | Yes | Material icons |
| `MatProgressSpinnerModule` | Yes | Yes | Yes | Loading indicators |
| `MatSnackBarModule` | Yes | Yes | Yes | Toast notifications |
| `MatDividerModule` | Yes | No | No | Visual separators |
| `MatTableModule` | No | No | Yes | Wallet list data table |

### 2.3 Angular Material Modules NOT In Use

| Module | Recommended Use Case | Priority |
|--------|---------------------|----------|
| `MatToolbarModule` | Top navigation bar, app header | High |
| `MatSidenavModule` | Sidebar navigation, dashboard layout | High |
| `MatMenuModule` | User menu, action menus | Medium |
| `MatDialogModule` | Confirmation dialogs, wallet details | Medium |
| `MatSelectModule` | Currency selection, dropdown filters | Medium |
| `MatCheckboxModule` | Terms acceptance, multi-select filters | Low |
| `MatTooltipModule` | Help text, info icons | Low |
| `MatChipsModule` | Status badges, tag displays | Low |
| `MatBadgeModule` | Notification counts, alerts | Low |
| `MatPaginatorModule` | Large data table pagination | Future |
| `MatSortModule` | Table column sorting | Future |
| `MatTabsModule` | Multi-section forms, dashboard tabs | Future |
| `MatExpansionModule` | FAQ sections, collapsible details | Future |
| `MatListModule` | Transaction lists, navigation items | Future |
| `MatGridListModule` | Dashboard KPI grids | Future |
| `MatAutocompleteModule` | Search, currency autocomplete | Future |
| `MatDatepickerModule` | Date range filters, transaction dates | Future |
| `MatRadioModule` | Single-choice selections | Low |
| `MatSlideToggleModule` | Feature toggles, settings | Low |

---

## 3. Visual Consistency Analysis

### 3.1 Hardcoded Hex Colors

#### `theme.scss` — 32 hardcoded hex values overriding `--mat-sys-*` tokens

```scss
--mat-sys-primary: #C89B3C;           // Gold 500
--mat-sys-on-primary: #0B1220;        // Neutral 900
--mat-sys-primary-container: #362B00; // Gold 900
--mat-sys-on-primary-container: #F4D161;
--mat-sys-secondary: #CBD5E1;         // Neutral 300
--mat-sys-on-secondary: #0B1220;      // Neutral 900
--mat-sys-secondary-container: #243146;
--mat-sys-on-secondary-container: #E2E8F0;
--mat-sys-tertiary: #3B82F6;          // Info blue
--mat-sys-on-tertiary: #FFFFFF;
--mat-sys-background: #0B1220;        // Neutral 900
--mat-sys-on-background: #F8FAFC;     // Neutral 50
--mat-sys-surface: #121B2E;
--mat-sys-on-surface: #F8FAFC;
--mat-sys-surface-variant: #1B2538;
--mat-sys-on-surface-variant: #CBD5E1;
--mat-sys-outline: #2A3448;
--mat-sys-outline-variant: #334155;
--mat-sys-error: #EF4444;             // Error red
--mat-sys-on-error: #FFFFFF;
--mat-sys-surface-container-lowest: #060C18;
--mat-sys-surface-container-low: #0F1729;
--mat-sys-surface-container: #121B2E;
--mat-sys-surface-container-high: #1B2538;
--mat-sys-surface-container-highest: #243146;
--mat-sys-surface-bright: #334155;
--mat-sys-inverse-surface: #E2E8F0;
--mat-sys-inverse-on-surface: #0B1220;
--mat-sys-inverse-primary: #695600;
```

**Recommendation:** Derive from `$aegis-colors` SCSS map using `map.get()` instead of hardcoding.

#### `_colors.scss` — 15+ hardcoded hex values in semantic aliases

Lines 109-145 define CSS custom properties with inline hex values that duplicate the `$aegis-colors` map. These should reference map values via `#{map.get(...)}` interpolation.

### 3.2 Spacing Consistency

| Component | Status | Notes |
|-----------|--------|-------|
| `auth.component.scss` | Good | All spacing uses `var(--aegis-space-*)` |
| `registration.component.scss` | Minor issue | `min-height: 100vh` vs `calc(100vh - var(--aegis-space-16))` in others |
| `wallet.component.scss` | Good | All spacing uses tokens |

### 3.3 Typography Consistency

All components properly use `var(--aegis-text-*)` tokens. No hardcoded font sizes found.

### 3.4 Inline Styles

| File | Line | Issue |
|------|------|-------|
| `wallet.component.html` | 12 | `style="text-transform: uppercase"` on currency input |

---

## 4. Accessibility Audit

### 4.1 Issues Found

| # | Severity | Component | Issue | WCAG Criterion |
|---|----------|-----------|-------|---------------|
| A1 | Medium | Auth, Registration | Missing `aria-label` on password visibility toggle buttons | 4.1.2 Name, Role, Value |
| A2 | Medium | All pages | Missing `<h1>` heading — all page titles are `<mat-card-title>` (`<h2>`) | 1.3.1 Info and Relationships |
| A3 | Low | Registration | Decorative `check_circle` icon lacks `aria-hidden="true"` | 1.1.1 Non-text Content |
| A4 | Low | All spinners | `<mat-spinner>` lacks `aria-label` for loading state | 4.1.3 Status Messages |
| A5 | Low | All cards | `<mat-card>` lacks explicit `role="region"` and `aria-labelledby` | 1.3.1 Info and Relationships |

### 4.2 Passing Checks

- All `<mat-form-field>` elements use `<mat-label>` (auto-associated with inputs)
- `lang="en"` present on `<html>` element
- Form validation errors use `<mat-error>` (accessible error association)
- ESLint includes `angular.configs.templateAccessibility`

---

## 5. Duplicate Pattern Analysis

### 5.1 Pattern: Card-Based Form Layout (3 occurrences)

**Found in:** Auth, Registration, Wallet

```html
<div class="*-container">
  <mat-card class="*-card">
    <mat-card-header>
      <mat-card-title>...</mat-card-title>
      <mat-card-subtitle>...</mat-card-subtitle>
    </mat-card-header>
    <mat-card-content>
      <form [formGroup]="..." (ngSubmit)="onSubmit()">
        <!-- form fields -->
        <button mat-raised-button type="submit" [disabled]="isLoading || form.invalid">
          @if (isLoading) { <mat-spinner diameter="20"></mat-spinner> }
          @else { Label }
        </button>
      </form>
    </mat-card-content>
  </mat-card>
</div>
```

**Recommendation:** Extract to `FormCardComponent` with `@Input()` for title, subtitle, formGroup, submitLabel, loading.

### 5.2 Pattern: Password Visibility Toggle (2 occurrences)

**Found in:** Auth, Registration

```html
<button mat-icon-button matSuffix (click)="hidePassword = !hidePassword" type="button">
  <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
</button>
```

**Recommendation:** Extract to `PasswordInputComponent` with internal state management.

### 5.3 Pattern: Loading Button (3 occurrences)

**Found in:** Auth, Registration, Wallet

```html
<button [disabled]="isLoading || form.invalid">
  @if (isLoading) { <mat-spinner diameter="20"></mat-spinner> }
  @else { Label }
</button>
```

**Recommendation:** Extract to `LoadingButtonComponent` with `@Input() loading, disabled, label`.

### 5.4 Pattern: Form Error Display (3 occurrences)

**Found in:** Auth, Registration, Wallet

```html
@if (form.get('field')?.invalid && form.get('field')?.touched) {
  <mat-error>{{ getErrorMessage('field') }}</mat-error>
}
```

**Recommendation:** Extract to `FormFieldErrorComponent` or use a template-driven approach.

### 5.5 Pattern: `getErrorMessage()` Method (3 occurrences)

All three components implement nearly identical switch-case error message logic.

**Recommendation:** Extract to shared `validation.utils.ts` utility function.

### 5.6 Pattern: `onSubmit()` Guard (3 occurrences)

All three components start with identical form validation guard and snackbar notification.

**Recommendation:** Extract to `FormSubmitGuard` utility or base class.

### 5.7 Pattern: Card SCSS Styling (2 occurrences)

`auth.component.scss` and `wallet.component.scss` share nearly identical card styling blocks.

**Recommendation:** Extract to shared SCSS mixin in `styles/mixins/_cards.scss`.

---

## 6. Component Architecture Assessment

### 6.1 Structural Quality

| Metric | Auth | Registration | Wallet | Verdict |
|--------|------|-------------|--------|---------|
| `ChangeDetectionStrategy.OnPush` | Missing | Missing | Missing | Needs fix |
| Subscription cleanup | Missing | Missing | Missing | Needs fix |
| `trackBy` on iterables | N/A | N/A | Missing | Needs fix |
| Inline styles | None | None | 1 instance | Minor |
| `inject()` pattern | Yes | Yes | Yes | Good |
| `finalize()` for loading | Yes | Yes | Yes | Good |
| Reactive forms | Yes | Yes | Yes | Good |
| Standalone component | Yes | Yes | Yes | Good |
| Lazy-loaded route | Yes | Yes | Yes | Good |

### 6.2 Service Architecture

| Service | Methods | HTTP Calls | Interceptor Support |
|---------|---------|-----------|-------------------|
| `AuthService` | 4 | 4 (POST/GET) | None |
| `RegistrationService` | 1 | 1 (POST) | None |
| `WalletService` | 3 | 3 (POST/GET) | None |

**Issues:**
- No `HttpInterceptor` for auth token injection, error normalization, or request logging
- Each component handles errors individually with `snackBar.open()`
- No centralized error handling strategy

### 6.3 Model Issues

| Issue | Files | Severity |
|-------|-------|----------|
| Duplicate `ErrorResponse` interface | `auth.model.ts`, `registration.model.ts` | Medium |
| `LoginResponse` may not match BFF contract (HttpOnly cookies) | `auth.model.ts` | Low |
| No barrel export (`index.ts`) for models | `shared/models/` | Low |
| No runtime validation (Zod/schema) | All models | Low |

### 6.4 Route Security

| Route | Auth Guard | Status |
|-------|-----------|--------|
| `/login` | N/A (public) | OK |
| `/register` | N/A (public) | OK |
| `/wallets` | **Missing** | HIGH RISK |

**Issue:** `/wallets` is publicly accessible without authentication. An `AuthGuard` with `canActivate` is needed.

---

## 7. Gap Analysis

### 7.1 Components Needed by Design System but Not Yet Present

| Component | Purpose | Priority | Depends On |
|-----------|---------|----------|-----------|
| `AppShellComponent` | Toolbar + sidenav layout wrapper | High | #31 Layout Redesign |
| `AppHeaderComponent` | Top navigation bar with logo, user menu | High | #31 Layout Redesign |
| `AppSidebarComponent` | Navigation sidebar with route links | High | #31 Layout Redesign |
| `AuthGuard` | Route protection for authenticated pages | High | #30 Theme |
| `HttpErrorInterceptor` | Centralized error handling | Medium | #30 Theme |
| `HttpAuthInterceptor` | Token injection for API calls | Medium | #30 Theme |
| `LoadingButtonComponent` | Reusable button with spinner | Medium | This audit |
| `FormCardComponent` | Reusable card-based form container | Medium | This audit |
| `PasswordInputComponent` | Password field with visibility toggle | Medium | This audit |
| `StatusChipComponent` | Semantic status indicator (success/warning/error) | Low | #33 Data Display |
| `EmptyStateComponent` | Empty state illustration with CTA | Low | #33 Data Display |
| `ConfirmDialogComponent` | Reusable confirmation dialog | Low | Future |
| `DatePipe` (custom) | Consistent date formatting | Low | Future |
| `CurrencyPipe` (custom) | Consistent currency display | Low | Future |

### 7.2 Missing Infrastructure

| Item | Purpose | Priority |
|------|---------|----------|
| `shared/components/` directory | Shared UI components | High |
| `shared/directives/` directory | Custom structural/attribute directives | Medium |
| `shared/pipes/` directory | Display formatting pipes | Low |
| `shared/utils/` directory | Validation, formatting utilities | Medium |
| `shared/interceptors/` directory | HTTP interceptors | High |
| `shared/guards/` directory | Route guards | High |
| `shared/models/error.model.ts` | Single ErrorResponse definition | Medium |
| `styles/mixins/` directory | SCSS mixins (card, button, form patterns) | Medium |

---

## 8. Migration Priority Matrix

### 8.1 Effort vs Impact Assessment

| # | Item | Effort | Impact | Priority | Phase |
|---|------|--------|--------|----------|-------|
| 1 | Add `AuthGuard` to `/wallets` route | Low | High | **P0** | Immediate |
| 2 | Fix `aria-label` on password toggles | Low | Medium | **P0** | Immediate |
| 3 | Add `<h1>` to all pages | Low | Medium | **P0** | Immediate |
| 4 | Add `ChangeDetectionStrategy.OnPush` | Low | Medium | **P1** | Theme (#30) |
| 5 | Add subscription cleanup (`takeUntilDestroyed`) | Low | Medium | **P1** | Theme (#30) |
| 6 | Extract duplicate `ErrorResponse` to shared model | Low | Medium | **P1** | Theme (#30) |
| 7 | Derive `theme.scss` hex values from token maps | Medium | High | **P1** | Theme (#30) |
| 8 | Derive `_colors.scss` semantic aliases from maps | Medium | High | **P1** | Theme (#30) |
| 9 | Create `shared/components/LoadingButtonComponent` | Medium | Medium | **P2** | Forms (#32) |
| 10 | Create `shared/components/FormCardComponent` | Medium | Medium | **P2** | Forms (#32) |
| 11 | Create `shared/components/PasswordInputComponent` | Medium | Low | **P2** | Forms (#32) |
| 12 | Extract `getErrorMessage()` to shared utility | Low | Low | **P2** | Forms (#32) |
| 13 | Create `HttpErrorInterceptor` | Medium | High | **P2** | Layout (#31) |
| 14 | Create `HttpAuthInterceptor` | Medium | High | **P2** | Layout (#31) |
| 15 | Fix `min-height` inconsistency in registration | Low | Low | **P3** | Layout (#31) |
| 16 | Remove inline `style` from wallet template | Low | Low | **P3** | Layout (#31) |
| 17 | Add `enableMockLogin: false` to prod environment | Low | Low | **P3** | Chore |
| 18 | Add wildcard 404 route | Low | Low | **P3** | Chore |
| 19 | Create `styles/mixins/_cards.scss` | Low | Low | **P3** | Layout (#31) |
| 20 | Add `aria-hidden` to decorative icons | Low | Low | **P3** | Accessibility (#35) |

### 8.2 Phase Mapping to Epic Sub-Tasks

| Phase | Sub-Task | Issues |
|-------|----------|--------|
| **Immediate** | This audit (#29) | 1, 2, 3 |
| **Theme (#30)** | Theme Implementation | 4, 5, 6, 7, 8 |
| **Layout (#31)** | Layout & Navigation Redesign | 13, 14, 15, 16, 19 |
| **Forms (#32)** | Form & Input Components | 9, 10, 11, 12 |
| **Data Display (#33)** | Tables, Cards, Chips | StatusChip, EmptyState |
| **Accessibility (#35)** | WCAG 2.1 AA Compliance | 20 + A1-A5 |
| **Chore** | Maintenance | 17, 18 |

---

## 9. Recommendations Summary

### 9.1 Immediate Actions (Before Theme Implementation)

1. Add `AuthGuard` to protect `/wallets` route
2. Add `aria-label` to password visibility toggle buttons
3. Add `<h1>` heading to all pages (upgrade `<mat-card-title>` or add wrapper)

### 9.2 During Theme Implementation (#30)

4. Add `ChangeDetectionStrategy.OnPush` to all components
5. Add subscription cleanup using `takeUntilDestroyed()`
6. Consolidate duplicate `ErrorResponse` interface
7. Refactor `theme.scss` to derive `--mat-sys-*` values from `$aegis-colors` SCSS map
8. Refactor `_colors.scss` semantic aliases to use `map.get()` interpolation

### 9.3 During Layout Redesign (#31)

9. Create `AppShellComponent` with toolbar + sidenav
10. Implement `HttpErrorInterceptor` for centralized error handling
11. Implement `HttpAuthInterceptor` for token management
12. Fix `min-height` and inline style inconsistencies

### 9.4 During Form Redesign (#32)

13. Extract `LoadingButtonComponent`, `FormCardComponent`, `PasswordInputComponent`
14. Create shared validation utilities

---

## 10. Component Usage Frequency

| Material Component | Usage Count | Components Using It |
|-------------------|-------------|-------------------|
| `mat-card` | 4 | Auth, Registration, Wallet (x2) |
| `mat-form-field` | 8 | Auth (2), Registration (4), Wallet (2) |
| `mat-raised-button` | 3 | Auth, Registration, Wallet |
| `mat-spinner` | 4 | Auth, Registration, Wallet (x2) |
| `mat-icon-button` | 2 | Auth, Registration |
| `mat-icon` | 5 | Auth (2), Registration (3), Wallet (0) |
| `mat-button` | 2 | Auth, Wallet |
| `mat-error` | 6+ | Auth, Registration, Wallet |
| `mat-table` | 1 | Wallet |
| `mat-divider` | 1 | Auth |

---

## 11. Test Coverage Summary

| Component | Spec Lines | Test Count | HTTP Mocks | Form Tests | Loading Tests |
|-----------|-----------|-----------|-----------|-----------|--------------|
| `AppComponent` | 29 | 3 | No | No | No |
| `AuthComponent` | 140 | 10 | Yes | Yes | Yes |
| `RegistrationComponent` | 165 | 12 | Yes | Yes | Yes |
| `WalletComponent` | 153 | 15 | Yes | Yes | Yes |

**Total:** 40 tests, all passing. Good coverage of HTTP mocking, form validation, and loading states.

---

## 12. Dependencies

### 12.1 Production Dependencies

| Package | Version | Purpose |
|---------|---------|---------|
| `@angular/*` | `^22.0.4` | Framework |
| `@angular/material` | `^22.0.0` | UI components |
| `@angular/cdk` | `^22.0.0` | Component Dev Kit |
| `rxjs` | `~7.8.0` | Reactive programming |
| `zone.js` | `~0.16.0` | Change detection |

### 12.2 Recommended Additions

| Package | Purpose | Priority |
|---------|---------|----------|
| `@ngrx/component-store` | Lightweight state management | Medium |
| `date-fns` | Date formatting utilities | Low |
| `zod` | Runtime schema validation | Low |

---

*This report was generated as part of Epic #27 — Visual Identity & Design System Overhaul.*
