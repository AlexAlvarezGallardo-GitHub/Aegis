# Evidence — UC-013 Frontend UI/UX Modernization (Unit / Frontend)

**Tier**: Unit (frontend Karma) + manual/e2e baseline for the UI modernization.
**Feature**: 013-frontend-ui-modernization
**Branch**: `feature/013-frontend-ui-modernization`
**Period**: 2026-08-09 → ongoing. This file is appended per phase (AGENTS.md rule: report every tier).

---

## Phase 0 — Baseline & safety net

**Scope**: capture the pre-change state (visual + engineering) so every phase can be diffed against it.

### Commands executed

| # | Command (workdir `frontend/aegis-frontend`) | Result |
|---|---------------------------------------------|--------|
| 1 | `npm run lint` | ✅ PASS — "All files pass linting." |
| 2 | `npm run build` | ✅ PASS — bundle generated (initial 527.22 kB raw / 104.43 kB transfer). ⚠️ **WARNING**: `wallet.component.scss` 8.51 kB > budget 6.00 kB (over by 2.51 kB). Action: refactor wallet SCSS in Phase 5. |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **159/159 SUCCESS** (1.355s). Zero failures. |
| 4 | `docker compose -f infra/docker-compose.yml up -d` | ✅ PASS — all services up: Postgres x5, Redis, Kafka, identity, wallet, reporting, fraud, audit, bff, kafka-ui, dbgate. BFF `:8082` and Identity `:8081` respond. |
| 5 | `git config core.hooksPath` | ✅ `.githooks` active |

### Visual baseline captured (`evidence/baseline/`)

| File | Screen | Viewport | Notes |
|------|--------|----------|-------|
| `00-login.png` | Login | 1440×900 | Old design: navy card, "Welcome back to Aegis", dev mock-login section |
| `01-register.png` | Registration | 1440×900 | Old design: "Create Your Account" |
| `02-shell-wallets-empty.png` | Shell + Wallets (empty) | 1440×900 | Old design: sidebar groups, header, badge **"3"** (fake), KPI row `$0.00`, empty state |
| `03-placeholder-transactions.png` | Placeholder route | 1440×900 | `PagePlaceholderComponent` inline-styled |
| `04-wallets-mobile-390.png` | Wallets mobile | 390×844 | Old design mobile layout |

### Baseline findings (confirmed live)

- Header/sidebar show fabricated notification counts (`Alerts 3`, bell badge `3`).
- Light-mode toggle present (target of D2 removal).
- Wallets empty state + `$0.00` total balance (single-currency display).
- Registration succeeded (201) but submit button stayed in `loading` state after success → **UX bug confirmed live** (backlog: T058/T059 family — stale UI after mutation). User `alex@aegis.test` registered in this stack's DB.
- Login flow works (→ `/wallets`); guard redirect works.

### Environment notes

- Docker stack's `aegis-frontend` nginx container serves an inconsistent build (index references assets that 404). The container was **stopped**; manual verification uses `ng serve` (dev, `:4200`) against the stack's BFF (`:8082`). Documented for later phases; not a code defect.

### Failures

None blocking. One pre-existing budget warning (wallet SCSS) tracked.

---

## Phase 1 — Design tokens & theme foundation (T1, foundational)

**Scope**: re-point token system to the portfolio language (zinc + gold `#d4af37` + Geist), dark-only theme, icon registry, new token categories.

### Changes

- **T005** `styles/tokens/_colors.scss`: navy → zinc scale (`#09090b`/`#18181b`/`#3f3f46`…), gold re-centered (`#d4af37` primary / `#e6c15a` accent), semantic aliases preserved, **new** `--aegis-surface-success` + `--aegis-border-success` (fixes deposit-receipt bug), duplicate gold 200/300 removed, light mixin removed.
- **T006** `_typography.scss`: added `--aegis-font-display` (Geist).
- **T007** New `styles/tokens/_layout.scss`: z-index, breakpoints (480/768/1024/1280), icon sizes, focus-ring tokens. New shared `src/app/shared/utils/breakpoints.ts` (`BREAKPOINTS`, `MOBILE_BREAKPOINT`).
- **T008** Aliases consolidated (single `--aegis-*` source; both alias families point at same values).
- **T009/T010** Dark-only: deleted `_light.scss`, light color mixin, `ThemeService`, `theme.service.spec.ts`, `theme-toggle` component; `themes/_index.scss` + `theme.scss` dark-only; static `<html data-theme="dark">`; `theme-color` → `#09090b`. Removed `<app-theme-toggle>` + import from sidebar.
- **T011** `_dark.scss`: imports palette from token maps (single source of truth); hardcoded `#3B82F6`/`#EF4444`/`#e8ecf4`/rgba → token refs / `#fafafa`.
- **T012** `IconRegistryService.register()` via `APP_INITIALIZER` in `app.config.ts`; added `src/assets` to `angular.json` build+test assets.
- **T013** `index.html`: Geist added to Google Fonts link (no new dependency).
- **T014** `styles.scss`: headings/KPI use display font; gold 30% selection; global `:focus-visible` ring token; `.tabular-nums` utility; scrollbar inherits zinc tokens.
- **T015** grep-gate cleanups: removed dead `var(--token, #hex)` fallbacks in `wallet.component.scss` (success/error signs, receipt surface/border).

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS — global CSS **182.50 kB → 68.11 kB**; initial total 527 → 428 kB. Pre-existing wallet SCSS budget warning unchanged (8.51 kB, tracked to Phase 5). |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** (was 159; −9 from removed theme specs). |
| 4 | Grep FR-001 (hex in component SCSS outside tokens) | ⚠️ 5 remaining, all pre-assigned: premium badge `#fbbf24/#d97706/#1a1a2e` (T038), sidebar `#fff` (T018), dialog `#FFFFFF` (T023), loading-button `#FFFFFF` (T022) |
| 5 | Grep FR-002 (undefined `var()`) | ✅ Zero undefined-token fallbacks |
| 6 | Live verification (Playwright, `:4200`) | ✅ `bodyBg rgb(9,9,11)`, `--aegis-color-primary #d4af37`, `--aegis-font-display Geist`, `data-theme="dark"`, surface-success/border-success defined. Screenshot: `evidence/unit/phase1-login-zinc.png` |

### Constitution re-check

Principle IV ✅ (no secrets; dark-only static; auth mechanics untouched). Principle V ✅ (150/150 unit green; specs updated/removed consistently).

### Failures

None. Remaining hex in component SCSS is tracked to specific Phase 3/5 tasks (not a Phase 1 regression).

---

## Phase 2 — Shell & Navigation (US-1, T2)

**Scope**: modernize the app shell — app-shell, sidebar, header, navigation, mobile drawer.

### Changes

- **T016** Layout dims tokenized (`--aegis-sidebar-width`, `-collapsed`, `--aegis-header-height`, `--aegis-nav-item-height`, `--aegis-content-max-width` in `_layout.scss`); content max-width 1280px + responsive padding.
- **T017** Mobile drawer fixed: sidebar off-canvas (`translateX(-100%)`, `position:fixed`, `aria-hidden`) on mobile, opens as overlay drawer; **separate `mobileOpen` state from the desktop `collapsed` rail** (was: 64px rail left in-flow on phones — the reported bug); backdrop click / Escape / route-change close; body scroll-lock; closes on desktop resize. New inputs/outputs on `app-shell`/`sidebar`; `MOBILE_BREAKPOINT` from shared `breakpoints.ts`.
- **T018** Sidebar: removed fake `Alerts 3` badge (A9) and the `NavItem.badge` plumbing; gold active pill kept; `:focus-visible` ring on nav items + toggle; brand mark/dims tokenized; collapse toggle only on desktop.
- **T019** Header: removed notification button + `MatBadgeModule` + fake `notificationCount(3)` (A9); breadcrumb now from route `data.title` (fallback to path segment); removed dead `.env-staging` CSS; dims/icons tokenized; avatar uses `--aegis-color-on-primary`.
- **T020** Page container: `.aegis-content` max-width + centered + responsive padding.
- **T021** Lint fix (overlay a11y key events) + phase gate.

### Bug found & fixed during implementation

`@media (max-width: var(--aegis-breakpoint-sm))` is **invalid CSS** — CSS custom properties don't work in media queries. Replaced with the SCSS map: `@media (max-width: #{map.get(tokens.$aegis-breakpoints, sm)})` in header + app-shell.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS (after overlay a11y fix) |
| 2 | `npm run build` | ✅ PASS (wallet budget warning unchanged, Phase 5) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |

### Live verification (Playwright)

| Check | Result |
|-------|--------|
| Desktop sidebar width 240px; no notification button; Alerts item without badge; active nav = Wallets | ✅ |
| Breadcrumb `/transactions` → **"Transactions"** (from `data.title`) | ✅ |
| Mobile 390px: sidebar `position:fixed`, `transform translateX(-241px)` (off-canvas), `aria-hidden=true`; hamburger visible | ✅ |
| Drawer open: transform 0, overlay present, `body.overflow=hidden` (scroll-lock), `aria-hidden=false` | ✅ |
| Escape closes; backdrop click closes; scroll restored | ✅ |
| Console errors: **0** | ✅ |

Screenshots: `evidence/unit/phase2-shell-wallets.png`. (Mobile drawer current evidence: `evidence/mobile/02-drawer-open.png` — see Scope Change 2.)

### Failures

None.

---

## Phase 3 — Shared Component Library (US-2, T3)

**Scope**: harden shared components — token-driven state coverage, focus/overlay fixes, page-placeholder rewrite.

### Changes

- **T022** `loading-button`: `:focus-visible` ring on ALL variants (was primary-only); `transition: all` → explicit properties; `#FFFFFF` → `--aegis-color-on-error`; danger label token.
- **T023** `confirmation-dialog`: overlay → `--aegis-surface-overlay`, z-index → `--aegis-z-modal`; `cdkTrapFocus` added to panel; destructive label → `--aegis-color-on-error`; overlay made keyboard-accessible (role button + keydown handlers) to satisfy a11y lint.
- **T024** `empty-state`: gold rgba border → `--aegis-gold-100`; icon size token; new `action` output so an action without a route emits instead of being a silent no-op (fixes dead-click).
- **T025** `status-chip`: neutral variant → `--aegis-color-neutral-bg` (new token) instead of hardcoded `rgba(100,116,139,0.12)`; icon size token.
- **T026** `stat-card`: variant gradients → semantic tokens (`gold-50`, `success-bg`, `warning-bg`, `error-bg`); value → display font; icon sizes tokenized.
- **T027** `toast-container`: icon uses `--aegis-icon-size-lg` (was spacing token as font-size); z-index → `--aegis-z-toast`.
- **T028** `loading-skeleton`: shimmer already token-driven (zinc) — verified.
- **T029** `page-placeholder` rewritten: reuses `app-empty-state` (icon, title from route, description); zero inline px/hex styles.
- **T030** `aegis-icon`: registry wired in T012; verified rendering.
- **T031** a11y lint fixes + phase gate.

### New tokens

`--aegis-color-on-error: #FFFFFF`, `--aegis-color-neutral-bg: rgba(148,163,184,0.12)`.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS (after 2 a11y lint fixes) |
| 2 | `npm run build` | ✅ PASS (wallet budget warning unchanged, Phase 5) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |
| 4 | Grep FR-001 | ⚠️ only 2 hex remain, both = wallet premium badge (T038, Phase 5) |

### Live verification (Playwright)

- Placeholder `/transactions`: renders `app-empty-state` (title "Transactions"), icon wrap border = `rgba(212,175,55,0.15)` (gold-100 token), **no inline styles**, console errors 0.

Screenshot: `evidence/unit/phase3-placeholder.png`.

### Failures

None.

---

## Phase 4 — Authentication (US-3, T4)

**Scope**: branded auth (login + registration) with the real Aegis logo, immediate navigation, registration↔login links, autocomplete/ARIA.

### Changes

- **Logo fix (reported by user)**: the frontend used a fake generic gold shield (`logo.svg`, Material `shield` icon). Replaced with the **real Aegis logo** from `Aegis-Portfolio/public/aegis-icon.png` → `frontend/public/assets/aegis-icon.png`; favicon replaced with the real `aegis-icon-128.png`; sidebar brand-mark now renders the real logo.
- **T032** Branded auth layout: zinc background with portfolio-style subtle grid + soft gold glow; card on `--aegis-surface-card` with hairline border + gold-tinted shadow; real logo 56px above card; display font on titles. Same treatment on login + registration.
- **T033** Removed `setTimeout(800)` navigation hack — navigate to `returnUrl` immediately on success; success toast persists (toast container is global).
- **T034** Registration: added "Already have an account? Sign in" link (form) + "Continue to Sign In" action (success state).
- **T035** `password-input` now exposes an `autocomplete` input (default `current-password`); login uses `current-password`, registration `new-password`; email `autocomplete="email"`, name fields `given-name`/`family-name`.
- **T036** Spec fix: registration spec lacked `provideRouter` → added; gate green.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS (after removing `autofocus` — a11y lint rule) |
| 2 | `npm run build` | ✅ PASS (wallet budget warning unchanged, Phase 5) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** (registration spec: +`provideRouter`) |

### Live verification (Playwright)

- Login: real logo `/assets/aegis-icon.png`, card bg `rgb(24,24,27)` (zinc card), hairline border, `autocomplete="email"`.
- Register: "Already have an account?" → `/login`; password `autocomplete="new-password"`.
- Login flow navigates immediately (no 800ms delay); console errors 0.

Screenshots: `evidence/unit/phase4-login-branded.png`, `evidence/unit/phase4-register-branded.png`.

### Failures

None.

---

## Phase 5 — Wallet Experience (US-4, T5)

**Scope**: wallet page (default authenticated page) — scannable balances, per-currency totals, disabled dead actions, tokenized panels/receipt, error state, SCSS budget fix.

### Changes

- **T037** Deposit receipt: uses `--aegis-surface-success`/`--aegis-border-success` (defined in T1) — dark-mode receipt renders correctly (was light-mint).
- **T038** Premium badge → gold tokens (`--aegis-gold-400→600` gradient, `--aegis-color-on-primary` text); last hex in component SCSS removed.
- **T039** Balance sign colors already tokenized (Phase 1); card/detail values now render with `aegisCurrency` pipe + `tabular-nums`.
- **T040** New shared `AegisCurrencyPipe` + `formatMoney()` in `src/app/shared/utils/currency.pipe.ts`; **per-currency aggregate total** (A7) — `totalBalances()` groups by currency, KPI shows `€ x · $ y`; removed misleading single `$` on mixed-currency total; removed duplicate `formatCurrency`/`formatBalance` logic.
- **T041** Transfer/Withdraw buttons → `disabled` + "Coming soon" tooltip (A8); Deposit card button now wired to open detail + deposit form (was a no-op).
- **T042** Slide-over panels: `cdkTrapFocus`, Escape closes both panels (`@HostListener`), z-index tokens, mobile full-width; panel/detail/deposit styles moved to shared `styles/components/_panels.scss`.
- **T043** Error state added: `loadError` signal + `app-empty-state` with Retry action (re-fetch); empty state's Create Wallet now wired via `action` output.
- **T044** **SCSS budget fixed**: wallet.component.scss 8.47 kB → **<6 kB** by extracting panel/detail/deposit styles to the global `_panels.scss` partial; build warning gone.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS — **no wallet SCSS budget warning** (8.47→<6 kB) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |
| 4 | Grep FR-001 | ✅ zero hex literals in component SCSS |

### Live verification (Playwright, live BFF)

- Empty state → Create Wallet → panel (focus trapped) → USD wallet created.
- Card: balance `$0.00` (pipe), Transfer/Withdraw **disabled**, KPI Total Balance `$0.00`.
- **Deposit E2E**: 150 USD via BANK_TRANSFER with unique ref → **receipt rendered** `rgba(34,197,94,0.12)` bg / `rgb(34,197,94)` border (semantic tokens), text "Last deposit: 150.00 USD from BANK_TRANSFER (ref: ...)".
- Escape closes the detail panel; console errors 0.

Screenshots: `evidence/unit/phase5-wallet-detail-receipt.png`, `evidence/unit/phase5-wallet-cards.png`.

### Failures

None.

---

## Phase 6 — Placeholder Routes (US-5, T6)

**Scope**: all 9+ placeholder routes render the tokenized placeholder pattern.

### Changes

- **T045/T046** Verified all routes render `app-empty-state` with route `data.title`. Found and fixed a real routing bug: the frontend route **`/api-keys` collided with the proxy context `/api`** → the dev server proxied it to identity and returned an error (broken navigation). Fixed by narrowing the proxy to `/api/v1` (all real API calls already use `/api/v1/*`) in `proxy.conf.json` and `proxy.conf.docker.json`.

### Live verification (Playwright)

All 10 routes (`payments, transactions, payouts, currencies, fraud, alerts, health, settings, users, api-keys`) → `app-empty-state`, correct title, **0 console errors**.

### Failures

None.

---

## Phase 7 — Responsive Design (US-6, T7)

**Scope**: intentional layouts at 1440/1024/768/390; no horizontal overflow.

### Changes

- **T047** Remaining literal breakpoints replaced with the SCSS token map (`_panels.scss` 768→sm; wallet grid 1024→md, 480→xs). Now zero literal `px` breakpoints in SCSS outside tokens.
- **T048** Verified mobile layouts: KPI grid 4→2→1, slide-panels full-width ≤768px (done in T042), auth/register cards fit at 390px, sidebar off-canvas drawer (T017).

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |

### Live verification (Playwright)

- Viewport matrix 1440/1024/768/390 × `wallets, transactions, login, register` (16 combos): **zero horizontal overflows**, 0 console errors.
- Mobile evidence (current): `evidence/mobile/01-wallets-mobile.png`, `evidence/mobile/04-toast-mobile.png` (see Scope Change 2).

### Failures

None.

---

## Phase 8 — Accessibility (US-7, T8)

**Scope**: keyboard navigation, focus management, semantic controls, contrast, reduced motion.

### Changes

- **T050** Skip-to-content link (first focusable, visually hidden until focused, jumps to `#aegis-main-content`; main got `id` + `tabindex="-1"`).
- **T051** Panels: `cdkFocusInitial` → replaced with explicit focus-into-panel (the browser click would steal it back with `rAF`; `setTimeout` wins) — focus enters the `slide-panel` (`tabindex="-1"`) on open; Tab cycling trapped by `cdkTrapFocus`; focus **restored to the trigger** on close (Escape/backdrop/button).
- **T052** Audit: all clickable divs already have `role`+`tabindex`+key handlers (overlays, palette, cheat-sheet, dialog) — no semantic-violating clickable divs.
- **T053** `mat-error` is auto-associated via `aria-describedby` (Material). Added explicit `aria-label` to wallet search input + deposit amount/source/reference inputs.
- **T054** Contrast spot-checks (WCAG luminance on live tokens): **all 9 checks ≥ 4.5:1 (AA)** — worst `muted/card 6.91`, gold accent/card `8.43`, on-gold/gold `9.46`.
- **T055** `aegis-shimmer` mixin now only animates under `prefers-reduced-motion: no-preference` (static background when reduced); `count-up` directive already respects reduced-motion (has specs).

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS (fixed missing `@use 'sass:map'` in wallet SCSS introduced in T047) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |

### Live verification (Playwright)

- First `Tab` on a page focuses the skip link.
- Opening the wallet detail panel moves focus inside; 20 Tab presses stay trapped in the panel; Escape restores focus to the trigger button.
- All contrast pairs AA.

### Failures

None.

---

## Phase 9 — UX Bug Backlog (T9)

**Scope**: close the audit backlog items (verify-after-build) and sweep for new defects.

### Backlog items — status

| Item | Status |
|------|--------|
| Mobile sidebar rail in-flow | ✅ fixed in Phase 2 |
| Fake notification count / warn-red badge / dead `.env-staging` | ✅ fixed in Phase 2 |
| `setTimeout(800)` login navigation | ✅ fixed in Phase 4 |
| Registration dead-end success | ✅ fixed in Phase 4 |
| Dead Transfer/Withdraw buttons | ✅ fixed in Phase 5 |
| Mixed-currency `$` total | ✅ fixed in Phase 5 |
| Broken deposit-receipt tokens | ✅ fixed in Phase 5 |
| `empty-state` no-op action | ✅ fixed in Phase 3 |
| Dead `aegis-*` icons | ✅ fixed in Phase 1/3 |

### New defect found & fixed (T065)

**Infinite loading / stale UI on registration** (first observed in Phase 0 baseline): the register POST returned **201** but the button stayed in `loading` forever and the success screen never appeared. Root cause: `RegistrationComponent` used `ChangeDetectionStrategy.OnPush` with **plain properties** (`isLoading`, `successResponse`) instead of signals — the template never re-rendered after the HTTP response. Fixed by converting both to `signal()` (login already used signals; that's why it worked). Spec updated to signal accessors.

### Live verification (Playwright)

- Registration (new user): after 201 → success message replaces the form, spinner resets, "Continue to Sign In" → `/login`; 0 console errors.
- Stale UI after deposit mutation: KPI Total Balance updates immediately ($300.00 → $325.00), success toast shown.
- Console errors 0.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |

### Failures

None.

---

## Phase 10 — Polish (T10)

**Scope**: restrained motion, hover consistency, no layout shift, portfolio alignment.

### Changes

- **T066** Transitions already tokenized (150–200ms scale); card hovers use `aegis-card-hover` (elevation + gold border); gold accent moments: active nav pill, focus rings, primary CTA, avatar. No decorative noise.
- **T067** Skeleton shimmer (reduced-motion aware), toast slide-in, panel slide-in, page-enter animation — all consistent easing.
- **T068** Wallet skeleton-card dims tuned to match real cards (gap `space-3`, min-height 184px).

### Live verification (Playwright)

- **CLS during `/wallets` load: 0.0001** (well below 0.1 threshold — no meaningful layout shift).

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |

### Failures

None.

---

## Phase 11 — Evidence Refresh (T11 / Gate G8)

**Scope**: regenerate all UI-facing evidence with the new design; leave non-UI evidence untouched; prove e2e green.

### Changes

- **T069** Live stack up; e2e suite re-run on the new build.
- **T070** Re-captured `evidence/01–08*.png` with the SAME filenames and scenarios (login filled, wallets list, detail deposit section, deposit form filled, deposit receipt `150 EUR BANK_TRANSFER UI-MODERNIZATION-001`, duplicate-reference rejected, create-wallet form, two wallets). Viewport 1440×900, dark theme.
- **T071** Regenerated `evidence/e2e/results.json` (8.6 kB) + `evidence/html-report/index.html` via `npx playwright test --config=playwright.config.ts` → **5/5 passed**.
- **T072** Updated `e2e/README.md` captions (kept structure + Spanish); clarified `02-wallets-premium` that the PREMIUM badge only appears when the backend flags the wallet as premium.
- **T073** Sweep: no docs/README/obsidian references to the old-UI screenshots; Kafka (`09–12`), `load*/`, `observability/` evidence untouched (timestamps unchanged).

### E2E suite result

`npx playwright test --config=playwright.config.ts` (workdir `e2e`) — **5 passed (22.8s)**:

1. auth: login form validates and signs in
2. auth: rejects invalid credentials
3. wallet: creates a wallet
4. wallet: deposits funds with source and reference (UC-004)
5. wallet: rejects duplicate deposit reference (idempotency → 409)

### Gate G8 — Evidence review

- All UI screenshots (`01–08`) regenerated on the new design; filenames/references intact.
- Zero tracked screenshots of the old UI remain.
- Non-UI evidence (Kafka, load, observability) untouched.

### Failures

None.

---

## Summary (all phases)

| Phase | Gate | Result |
|-------|------|--------|
| 0 Baseline | quality | ✅ lint/build/test baseline recorded; 5 baseline screenshots |
| 1 Tokens & theme | quality + Constitution | ✅ zinc+gold+Geist, dark-only, registry; CSS 182→68 kB |
| 2 Shell & nav | quality + responsive | ✅ mobile drawer fixed, no fake data, breadcrumb real |
| 3 Components | quality | ✅ states/focus/overlays tokenized; placeholder rewritten |
| 4 Authentication | quality | ✅ real logo, no setTimeout, reg/login links, autocomplete |
| 5 Wallets | quality + deposit E2E | ✅ per-currency totals, disabled actions, receipt fixed, SCSS budget <6kB |
| 6 Placeholders | quality | ✅ 10 routes; /api-keys proxy collision fixed |
| 7 Responsive | quality + matrix | ✅ zero overflow at 1440/1024/768/390 |
| 8 Accessibility | quality | ✅ skip-link, focus mgmt, AA contrast, reduced-motion |
| 9 UX bugs | quality | ✅ registration infinite-loading bug fixed (signals) |
| 10 Polish | quality | ✅ CLS 0.0001 |
| 11 Evidence | **G8** | ✅ e2e 5/5, evidence regenerated, no stale screenshots |

**Final engineering state:** lint clean · build clean (budgets respected) · unit **150/150** · e2e **5/5** · 0 console errors · grep gates FR-001/FR-002 clean.

---

## Scope Change — Wallet Detail Full-Page Refactor (Product request)

**Request**: transform Wallet Detail from a right-side drawer into a dedicated full-page experience; deposit/withdraw as modal dialogs; adjust balance as an administrative operation. Reuse the existing design system; preserve functionality and API contracts.

### Changes

- **Route** `wallets/:walletId` → new `WalletDetailComponent` (lazy, inside the app shell); Wallet list "View/Deposit" now navigate to the detail page instead of opening a drawer.
- **`WalletDetailComponent`** (`features/wallet/wallet-detail/`): back nav `← Wallets`, header (currency wallet name, full ID, status chip, primary actions), 3-card balance overview (Available Balance / Currency / Status), and 5 tabs — **Overview, Transactions, Deposits & Withdrawals, Activity, Audit Log** — implemented as real application state via `?tab=` query params (navigable, default `overview`).
- **`MoneyDialogComponent`** (CDK Dialog): `deposit` (amount + source + reference), `withdraw` (amount + reason), `adjust` (amount + reason + reference **with admin warning banner**); focus-trapped, responsive ≤480px; confirm button shows the computed amount (`Deposit $150.00`).
- **Administrative separation**: Deposit/Withdraw are primary header actions; **Adjust Balance, Freeze (SUSPENDED), Deactivate (CLOSED)** and **View Audit Log** live under `More actions ⋮`; lifecycle changes require a confirmation dialog.
- **Activity store** (session-scoped, `WalletService.activities`): records real completed/rejected deposit, withdrawal and adjustment operations → powers Overview recent activity, Transactions table and Activity tab. **No backend data model invented** (backend exposes no transaction/activity endpoints — those tabs show honest empty states when the session has no activity; Audit Log explains it is server-side).
- **Compact toast**: `toast-message` now splits `\n` into a bold title + muted subtitle (e.g. `Deposit completed` / `$75.00 · BANK_TRANSFER`).
- Wallet list component simplified (drawer detail, inline deposit/adjust forms and `lastDeposit` receipt removed).

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS (wallet-detail SCSS trimmed to stay under 6 kB budget) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |
| 4 | `npx playwright test --config=playwright.config.ts` (e2e) | ✅ PASS — **6/6** (added "navigates to the full-page wallet detail"; deposit/duplicate-ref tests updated to the modal flow) |

### Live verification (Playwright)

- Detail is a **full page** (`/wallets/<id>`), no `.slide-panel` drawer; header shows "USD Wallet" + ID + status; overview 3 cards; all 5 tabs present; tab switch updates the URL (`?tab=transactions`).
- Deposit modal: dialog opens, submit → compact toast "Deposit completed / $75.00 · BANK_TRANSFER", balance updates ($600→$675).
- Adjust Balance (More actions) → dialog with admin warning + required reason.
- Back link returns to `/wallets`.
- Mobile 390px: overview stacks to 1 column, no horizontal overflow, modal fits viewport.
- Evidence `03–06` re-captured for the new UI; `e2e/README.md` captions updated. 0 console errors.

### Acceptance criteria check

Wallet Detail is no longer a drawer ✅ · full-page experience ✅ · header with name/ID/status/primary actions ✅ · balance immediately visible ✅ · Deposit/Withdraw primary + modal ✅ · Adjust Balance administrative ✅ · Overview/Transactions/Deposits & Withdrawals/Activity/Audit Log separated ✅ · recent activity on Overview ✅ · transactions easy to scan (table) ✅ · existing functionality works ✅ · API contracts preserved ✅ · visual identity preserved ✅ · desktop + mobile ✅ · no TS/build/lint errors ✅ · tests pass ✅.

### Failures

None.

---

## Scope Change 2 — Genuinely Responsive Mobile Experience (Product request)

**Request**: the desktop layout was rendered inside narrow viewports instead of providing a real mobile layout. Requirements: no page-level horizontal scrolling, mobile app shell (drawer instead of sidebar), mobile header, toast that never covers navigation, wallets/detail/modals/table responsive, touch targets ≥44px, tablet usable, desktop intact.

### Root cause found

The layout was "responsive" at the page level, but at **tablet width (768–1023) the fixed 240px desktop sidebar stayed visible**, leaving ~527px for content that then overflowed to the right and was silently clipped by `overflow:hidden` (diagnosed live: `.aegis-content` right edge at 786px on a 768px viewport).

### Changes

- **Breakpoints**: `MOBILE_BREAKPOINT` 768 → **1024** — the sidebar becomes an off-canvas drawer below desktop (≥1024px = desktop sidebar), matching the requested Desktop/Tablet/Mobile split.
- **Mobile app shell**: below 1024 the desktop sidebar is never shown in-flow; hamburger opens the drawer with the same sections (Payments / Wallets / Monitoring / Settings); closes on backdrop click, on selection (navigates), and on Escape; scroll-lock while open.
- **Mobile header**: two-row layout — row 1 `[☰] AEGIS · [Search] [avatar]`, row 2 breadcrumb (truncates with ellipsis); wide search trigger hidden on mobile, icon-only search button opens the command palette; env badge hidden on mobile; hamburger/search/user touch targets 44px.
- **Toast**: on mobile positioned **below the header** (new `--aegis-header-height-mobile: 88px`), `max-width: calc(100vw - 24px)`, centered, wraps text — never covers navigation; compact two-line format.
- **Wallets page**: KPI grid collapses to 1 column on mobile (`<768`); search field `width:100%; min-width:0`; Create Wallet button wraps below the title (never clipped); wallet cards `min-width:0`; action buttons 40px (44px on mobile).
- **Wallet detail**: overview cards stack to 1 column on mobile; balance/typography scale down without breaking hierarchy; header actions wrap; tabs scroll internally (no page-level scroll); transaction table scrolls inside its container.
- **Modals**: near full-width on small screens (`<480px` overlay padding 12px).
- **Global**: `min-width:0` added to grid/flex children (stat-card, wallet-card, overview cards) to stop content-forced overflow; long IDs/amounts use `overflow-wrap: anywhere`.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS (wallet-detail SCSS kept under the 6 kB budget) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **150/150** |
| 4 | `npx playwright test --config=playwright.config.ts` | ✅ PASS — **6/6** |

### Live validation (Playwright) — 6 viewports

| Viewport | Wallets overflow | Detail overflow | Sidebar |
|----------|-----------------|-----------------|---------|
| 375×812 | ✅ no | ✅ no | drawer (off-canvas) |
| 390×844 | ✅ no | ✅ no | drawer |
| 412×915 | ✅ no | ✅ no | drawer |
| 768×1024 | ✅ no | ✅ no | drawer (tablet fixed) |
| 1024×768 | ✅ no | ✅ no | desktop sidebar |
| 1440×900 | ✅ no | ✅ no | desktop sidebar |

- Mobile header = 2 rows (85px) with breadcrumb on its own row; desktop = 1 row (57px).
- Toast on 390px: `top:96` > header bottom `85` (below header), within viewport, `max-width: calc(100vw - 24px)`.
- Drawer flow verified: open → sections present → close via backdrop → open → select "Transactions" → navigates + closes.
- 0 console errors across all viewports.

### Acceptance criteria check

No horizontal page scrolling ✅ · no clipped content ✅ · no overlapping header elements ✅ · sidebar becomes mobile navigation ✅ · search works ✅ · create wallet works ✅ · cards fit ✅ · detail fits ✅ · deposit/withdraw/adjust modals fit ✅ · tables don't overflow page ✅ · toasts don't cover navigation ✅ · ≥44px touch targets on mobile ✅ · desktop intact ✅ · tablet usable ✅ · genuinely responsive ✅ · business logic/APIs unchanged ✅ · no TS/build/lint errors ✅ · tests pass ✅.

### Failures

None.

---

## Scope Change 3 — Refined Toast Notification System (Product request)

**Request**: the toast looked like a wide top-right banner competing with the header. Wanted: subtle bottom-right (desktop) / bottom-center (mobile) transient feedback with a clear hierarchy, financial context, capped stacking, and proper ARIA.

### Changes

- **`ToastService`**: richer model — `Toast { type, title, description?, metadata?, action?, duration, dismissible }`; API `success/error/warning/info(title, opts)`; **per-type default durations** (success 4000, info 4500, warning 6000, error 7000; `duration: 0` = sticky); **max 3 visible** (oldest dropped via `slice(-3)`); manual `dismiss`/`dismissAll` kept.
- **`ToastContainerComponent`**:
  - Position: **bottom-right** desktop; **bottom-center** mobile with safe margins (`width: calc(100vw - 24px)`, `max-width: calc(100vw - 32px)`).
  - Style: dark card, hairline border, subtle shadow, **3px left accent** in the status color (not a full colored border); small status icon; no glow/gradient.
  - Hierarchy: title (semibold) → description → metadata (mono) → optional action (`View transaction →`, `Copy`); small close button (44px touch target on mobile).
  - ARIA: `role="status"` for success/info, `role="alert"` for error/warning; `aria-live="polite"` container.
  - Animation: 150ms fade + 6px slide-in, `prefers-reduced-motion` respected; no bounce.
- **Call sites updated** to the new API: login/registration welcome messages (title + description), wallet created (title + `ID` metadata + **Copy** action → clipboard + "ID copied" info toast), deposits/withdrawals (title + `+$150.00 · BANK_TRANSFER` description + **View transaction** action → `?tab=transactions`), errors (title + friendly description, no raw API text), 409 duplicate reference, freeze/deactivate status toasts.
- Specs updated for the new API (+ `role status/alert` and `max 3` assertions).

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **152/152** |
| 4 | `npx playwright test` (e2e, dedicated user `e2e-wallet@aegis.test`) | ✅ PASS — **6/6** |

> Note: the e2e `creates a wallet` test failed against `alex@aegis.test` because that user reached the backend's **5-wallet limit** from repeated runs ("User cannot have more than 5 wallets" — the error toast worked as intended). Validated green with a dedicated test user.

### Live verification (Playwright)

- Desktop (1440×900): toast at bottom-right (left 1060 / bottom 880), compact (~388px), title "Deposit completed", description "+$150.00 · BANK_TRANSFER", action "View transaction" navigates to `?tab=transactions`, `role="status"`, no full colored border.
- Mobile (390×844): toast bottom-centered (left 12 / right 378, within viewport), `max-width: calc(100vw - 24px)`, close button 44px.
- Wallet-limit error toast renders correctly (backend 409 → "User cannot have more than 5 wallets").
- 0 console errors.

### Acceptance criteria check

Not a large top-right banner ✅ · does not overlap header ✅ · compact ✅ · bottom-right desktop ✅ · responsive mobile ✅ · success/error/warning/info variants ✅ · financial ops show amount + context ✅ · IDs secondary + Copy ✅ · auto-dismiss per type ✅ · manual dismiss ✅ · stacking + max 3 ✅ · no excessive glow/animation ✅ · Aegis identity preserved ✅ · nothing broken ✅ · no TS/build/lint errors ✅.

### Failures

None.

---

## Scope Change 4 — Hide Internal Wallet UUIDs from Primary UI (Product request)

**Request**: the wallet GUID/UUID was displayed in the main Wallets experience; only its presentation changes (internal/API usage preserved).

### Classification (searched `walletId` / `shortId` / UUID render sites)

- **A. Internal/backend** (preserved): `wallet.service.ts` URLs, route param `/wallets/:walletId`, `track` keys, activity `reference` (business references).
- **B. Primary user-facing UI** (changed): wallet card `shortId(wallet.walletId)`, wallet-detail header `detail-id`, money-dialog `Wallet: … · {id}`, wallet-creation toast `ID …`, search-by-walletId.
- **C. Administrative/technical**: new **Technical details** dialog (under `More actions`) exposing Wallet ID + Copy, Currency, Created, Updated, Owner ID.
- **D. Logs/debug**: none exposed.

### Changes

- **Wallet cards**: GUID removed → shows **Last activity** (type + signed amount + date) when available, else `Created {date}`. No artificial identifier substituted.
- **Wallet detail**: header shows only `USD Wallet` + status chip; balance card shows `Last activity · Deposit +$150.00 · date` when available (else "Last updated").
- **Wallet creation toast**: `USD wallet created successfully` (currency), no ID, no Copy on the toast.
- **Search**: filters by currency only (UUID removed from the search predicate).
- **Money dialog**: `Wallet: USD` (no UUID).
- **Technical details** (secondary, under More actions): dialog with Wallet ID (mono) + Copy (copies silently → "Wallet ID copied" toast without the UUID), Currency, Created, Updated, Owner ID. De-emphasized styling.

### Commands executed

| # | Command | Result |
|---|---------|--------|
| 1 | `npm run lint` | ✅ PASS |
| 2 | `npm run build` | ✅ PASS (wallet SCSS kept under budget) |
| 3 | `npx ng test --watch=false --browsers=ChromeHeadless` | ✅ PASS — **152/152** |
| 4 | `npx playwright test` (e2e, `e2e-wallet@aegis.test`) | ✅ PASS — **6/6** |

### Live verification (Playwright)

- Card meta: "Created Aug 9, 2026" (no UUID); full card text has no UUID.
- Create-wallet toast: "JPY wallet created successfully" (no ID).
- Detail header: "JPY Wallet", no UUID.
- Technical details dialog: Wallet ID + Copy present (UUID only here, secondary).
- Money dialog: "Wallet: JPY" (no UUID).
- 0 console errors.

### Acceptance criteria check

No UUID on cards ✅ · none in primary detail ✅ · none in creation toasts ✅ · none in activity lists ✅ · IDs available internally ✅ · API contracts preserved ✅ · deposit/withdraw work ✅ · selection/routing work ✅ · technical ID available via secondary context ✅ · no technical identifier substituted for business info ✅ · no TS/build/lint errors ✅ · tests pass ✅.

### Failures

None.

---

## Evidence Classification (by feature)

Evidencia regenerada con la UI actual y clasificada por feature (commit/PR) en `evidence/README.md`:

| Feature / PR | Commit | Capturas |
|--------------|--------|----------|
| Design system & app shell | `55b0138` | `01-login-filled`, `02-wallets`, `07-create-wallet-form`, `08-two-wallets` |
| Wallet Detail full-page | `bfb86a1` | `03-wallet-detail`, `04-deposit-modal` |
| Responsive mobile | `83dd3ba` | `mobile/01-wallets-mobile`, `mobile/02-drawer-open`, `mobile/03-wallet-detail-mobile`, `mobile/04-toast-mobile` |
| Toast system | `5f31de0` | `toast/01-toast-creation`, `toast/02-toast-deposit`, `toast/03-toast-error`, `toast/04-toast-stack` |
| UUID cleanup | `45c36ac` | `uuid/technical-details-dialog` (+ `02-wallets`/`03-wallet-detail` regeneradas sin UUID) |
| Flujos e2e (login, depósito, 409) | — | `05-deposit-success-toast`, `06-dup-reference-rejected` |
| Kafka / observability (no frontend) | — | `09-…`–`12-…`, `observability/` |

**Mobile** (390×844, feature responsive): wallets (header 2 filas + 1 col), drawer abierto, wallet detail, toast bottom-center.
**Toast** (feature toast): creación sin UUID, depósito +$150.00 · BANK_TRANSFER + View transaction, error 409, stacking vertical.
**UUID**: dialog técnico bajo More actions con Wallet ID + Copy (único lugar con UUID).

Regla aplicada: cuando una feature modifica una pantalla ya capturada, la captura se re-genera (mismo escenario, nuevo estado). Sin screenshots obsoletos en git.
