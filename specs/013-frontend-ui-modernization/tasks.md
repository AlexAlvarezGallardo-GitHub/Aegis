# Tasks: UC-013 Frontend UI/UX Modernization

> **Status note (2026-08-10):** the UI modernization (UC-013) shipped; the **responsive mobile layer was reverted** in `refactor/014-remove-mobile` — the app is desktop-only until mobile becomes a real feature. Tasks below referencing the mobile drawer/header/toast (T017, T042, T048, T056, responsive checkpoints) are **superseded** and should not be re-applied without a team decision. See `evidence/unit/ui-modernization-unit.md` (Refactor 014).

**Input**: Design documents from `specs/013-frontend-ui-modernization/` (spec.md, research.md, plan.md, quickstart.md)

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md

**Organization**: Tasks grouped by user story for independent implementation/testing. Groups map to plan task groups T0–T11. Each phase ends with a **Checkpoint** = blocking quality gate (lint → build → test → **grep gates: zero hex literals in component SCSS outside `styles/tokens/` + zero undefined `var()` references** → manual run against BFF `:8082` → responsive spot-check → evidence note in `evidence/unit/ui-modernization-unit.md`).

**Format**: `[ID] [P] [Story] Description` — `[P]` = can run in parallel (different files), `[Story]` = user story traceability.

---

## Phase 0: Setup & Baseline (T0)

**Purpose**: Safety net before any change; prove the starting point is green.

- [ ] T001 Baseline screenshot inventory: login, registration, wallets (populated/empty/loading/error), each placeholder route — at 1440/768/390, saved under `evidence/baseline/`
- [ ] T002 Record baseline results: `npm run lint`, `npm run build`, `npm test` in `frontend/aegis-frontend/`; note any pre-existing failures
- [ ] T003 Verify live stack: BFF reachable on `:8082` (per `quickstart.md` step 1/2)
- [ ] T004 [P] Verify git hooks active (`git config core.hooksPath .githooks`) and branch naming convention

**Checkpoint**: baseline committed; the phase-gate checklist is recorded for all subsequent groups.

---

## Phase 1: Foundational — Design tokens & theme (T1) ⚠️ CRITICAL — BLOCKS ALL USER STORIES

**Purpose**: The token layer every other phase consumes. No user-story work can begin until this phase is green.

### Tokens (parallel, different files)

- [ ] T005 [P] [FOUND] Re-point `src/styles/tokens/_colors.scss` to portfolio values per `research.md` §4: zinc scale, gold base `#d4af37` / accent `#e6c15a`, hairline borders, `#fafafa`/`#a1a1aa` text; keep all `--aegis-*` semantic alias names; define the missing `--aegis-surface-success` and `--aegis-border-success`; fix duplicate gold 200/300 and hex casing
- [ ] T006 [P] [FOUND] `src/styles/tokens/_typography.scss`: add `--aegis-font-display` (Geist) + `font-variant-numeric: tabular-nums` utility for financial figures
- [ ] T007 [P] [FOUND] New token categories in `src/styles/tokens/` + `_index.scss`: z-index scale, breakpoints (480/768/1024/1280 — SCSS vars + one shared TS constant in `src/app/shared/utils/`), icon-size scale, focus-ring token (2px `#d4af37`, offset 2px)
- [ ] T008 [P] [FOUND] Consolidate legacy `--aegis-*` aliases to a single naming scheme; codemod consumers found by grep

### Theme (dark-only, D2)

- [ ] T009 [FOUND] Remove light theme: delete `src/styles/themes/_light.scss` and the light color-token mixin; set static `<html data-theme="dark">` in `src/index.html`; `theme-color` → `#09090b`
- [ ] T010 [FOUND] Remove `ThemeService` + `theme-toggle` component + `prefers-color-scheme` listener; grep for consumers (`ThemeService`, `data-theme`, `theme-toggle`, `theme.service`); update/remove affected specs (`theme.service.spec.ts`, header spec)
- [ ] T011 [FOUND] `src/styles/themes/_dark.scss`: import palette from `tokens/_colors.scss` (single source of truth); replace hardcoded `#3B82F6`/`#EF4444`/`#FFFFFF` with token refs

### Assets & global

- [ ] T012 [FOUND] Wire `IconRegistryService.register()` via `APP_INITIALIZER` in `src/app/app.config.ts`; verify `aegis-*` icons render (kills 404s)
- [ ] T013 [FOUND] `src/index.html`: add Geist to the existing Google Fonts `<link>` (no new dependency)
- [ ] T014 [FOUND] `src/styles.scss`: focus-visible ring token, selection → gold 30%, scrollbar/borders → zinc hairlines

### Validation

- [ ] T015 [FOUND] Update/extend affected Karma specs (token-driven theming, dark-only, registry), then run the full phase gate

**Checkpoint**: `npm run lint` + `npm run build` (budgets) + `npm test` green; Constitution re-check (Principle IV/V); visual diff vs. baseline (palette shift, nothing broken).

---

## Phase 2: User Story 1 — Application Shell & Navigation (Priority: P1) 🎯 MVP

**Goal**: Shell/sidebar/header look and behave like a modern premium product.

**Independent Test**: Open any authenticated page → verify zinc shell, gold active-nav, mobile drawer, navigation intact.

- [ ] T016 [P] [US1] Tokenize all layout dims (sidebar 240/64px, header 56px, nav 40px) and zinc surfaces/hairlines in `src/app/shared/layout/app-shell/` + `sidebar.component.scss`
- [ ] T017 [US1] Fix mobile rail bug: sidebar hidden off-canvas on ≤768px, opens as overlay drawer with backdrop + scroll-lock, closes on Escape/backdrop/route-change (`app-shell.component.ts`, `app-shell.component.html/scss`, `sidebar.component.*`)
- [ ] T018 [P] [US1] Sidebar active-nav gold-pill + hover/focus-visible states; collapsed-rail tooltips retained (`sidebar.component.scss`)
- [ ] T019 [US1] Header: remove notification bell + badge (A9), breadcrumb from route `data.title`, remove dead `.env-staging`, user menu to tokens (`header.component.*`)
- [ ] T020 [P] [US1] Page container max-width + spacing scale (`src/styles.scss`, `.page-container`/`.content-container`)
- [ ] T021 [US1] Update header/app-shell/sidebar specs; run phase gate + responsive smoke (1440/768/390, drawer verified on touch viewport)

**Checkpoint**: shell fully functional and testable on its own — this is the visual MVP increment.

---

## Phase 3: User Story 2 — Shared Component Library (Priority: P1)

**Goal**: Consistent, hardened shared components with token-driven state coverage.

**Independent Test**: Exercise shared components on existing pages and verify hover/focus-visible/disabled/loading states.

- [ ] T022 [P] [US2] `loading-button`: focus ring on ALL variants, remove `transition: all`, `#FFFFFF` → `--aegis-color-on-primary`, spinner size token (`src/app/shared/forms/loading-button/`)
- [ ] T023 [P] [US2] `confirmation-dialog`: overlay → `--aegis-surface-overlay`, destructive label token, CDK `A11yModule` focus trap (`src/app/shared/components/confirmation-dialog/`)
- [ ] T024 [P] [US2] `empty-state`: gold rgba border → token; hide action button when no handler (`src/app/shared/data-display/empty-state/`)
- [ ] T025 [P] [US2] `status-chip`: neutral variant → semantic tokens; `reduced-motion` safe (`src/app/shared/data-display/status-chip/`)
- [ ] T026 [P] [US2] `stat-card`: variant gradients → tokenized tints, value → display font (`src/app/shared/data-display/stat-card/`)
- [ ] T027 [P] [US2] `toast-container`: icon size via text token, not spacing token
- [ ] T028 [P] [US2] `loading-skeleton`: shimmer tuned for zinc surfaces
- [ ] T029 [P] [US2] `page-placeholder`: rewrite with tokens, reuse `empty-state` (kills inline px/hex in `page-placeholder.component.ts`)
- [ ] T030 [P] [US2] `aegis-icon`: verify custom `aegis-*` SVGs render (depends on T012)
- [ ] T031 [US2] Add/extend specs for state behavior (disabled, loading transitions, focus ring); run phase gate

**Checkpoint**: component library consistent — all pages inherit it.

---

## Phase 4: User Story 3 — Authentication (Priority: P2)

**Goal**: Login + registration feel secure, modern, premium, simple.

**Independent Test**: Register + login against the live BFF stack; verify validation, loading, success navigation, errors.

- [ ] T032 [US3] Branded auth layout: zinc bg + restrained grid/glow, card on `--aegis-surface-card`, hairline border, responsive ≤390px (`src/app/features/auth/auth.component.*`)
- [ ] T033 [US3] Remove `setTimeout(800)` login navigation — navigate on success immediately, toast persists; loading button resets via `finalize` on success/error/timeout (`auth.component.ts`)
- [ ] T034 [US3] Registration: "Already have an account? Sign in" link; success state primary action "Continue to sign in" (`src/app/features/registration/registration.component.*`)
- [ ] T035 [US3] `autocomplete` hints, autofocus first field, label/error ARIA association on both forms
- [ ] T036 [US3] Update auth/registration specs; run phase gate + full manual flow (register → login → wallets → logout → guard redirect; invalid-submit toast; loading disabled states)

**Checkpoint**: auth flows intact and premium-looking; no behavioral regressions.

---

## Phase 5: User Story 4 — Wallet Experience (Priority: P2)

**Goal**: Wallet landing page (default post-login) — financial info easy to scan and deliberate to act on.

**Independent Test**: Against live stack — create wallet, view list, deposit end-to-end incl. 409 duplicate-reference.

- [ ] T037 [US4] Fix deposit receipt: `--aegis-color-success-bg/-text` + defined surface/border tokens (kills light-mint block in dark) (`wallet.component.scss`)
- [ ] T038 [P] [US4] Premium badge → gold token gradient, text → `--aegis-color-on-primary` (`wallet.component.scss`)
- [ ] T039 [P] [US4] Balance sign colors → `--aegis-color-success/-error` tokens; mono + `tabular-nums` on amounts
- [ ] T040 [US4] New shared `currency.pipe.ts` in `src/app/shared/utils/`; per-currency aggregate total (A7: `€ 150.00 · $ 200.00`), remove unconditional `$` on `totalBalance`; pipe reused by cards/KPIs/receipt
- [ ] T041 [US4] Transfer/Withdraw → disabled + "Coming soon" tooltip (A8); `matTooltipDisabled` handling on disabled buttons
- [ ] T042 [US4] Slide-over panels (create/deposit): tokenized surfaces, focus trap, Escape/backdrop close, full-width ≤480px (`wallet.component.*`)
- [ ] T043 [US4] States: skeletons match content (no CLS), modernized empty state, error state with retry (re-fetch)
- [ ] T044 [US4] Update wallet specs; run phase gate + manual deposit flow (incl. 409 duplicate-reference toast)

**Checkpoint**: wallet page fully functional, scannable, and consistent with the design system.

---

## Phase 6: User Story 5 — Placeholder Pages Consistency (Priority: P3)

**Goal**: 9 placeholder routes feel finished and branded.

**Independent Test**: Visit each placeholder route → tokenized placeholder (icon, route title, "In development").

- [ ] T045 [US5] Verify all 9 routes (`payments`, `transactions`, `payouts`, `currencies`, `fraud`, `alerts`, `health`, `settings`, `users`, `api-keys`) render the modernized placeholder with route `data.title`
- [ ] T046 [US5] Confirm zero inline styles with literal px/hex remain in `page-placeholder`

**Checkpoint**: every route consistent.

---

## Phase 7: User Story 6 — Responsive Behavior (Priority: P2)

**Goal**: Intentional layouts at 1440/1280/1024/768/480/390, no horizontal overflow.

**Independent Test**: Walk main flows at each viewport; verify no overflow and usable interactions.

- [ ] T047 [US6] Replace scattered breakpoints with breakpoint tokens/constant (`app-shell`, grids, panels)
- [ ] T048 [US6] Fix overflow/truncation: long balances + wallet IDs ellipsis + `title`; auth card padding; command palette width; KPI grid 4→2→1
- [ ] T049 [US6] Update affected specs; run phase gate + viewport matrix screenshots

**Checkpoint**: no horizontal overflow anywhere; mobile layouts intentional.

---

## Phase 8: User Story 7 — Accessibility (Priority: P3)

**Goal**: Keyboard-operable, perceivable, with practical ARIA.

**Independent Test**: Keyboard-only walkthrough (login → wallets → deposit → dialogs → palette) + contrast/focus audit.

- [ ] T050 [US7] Skip-to-content link as first focusable element
- [ ] T051 [US7] Keyboard audit: every interactive element reachable with gold focus ring; dialogs/panels trap + restore focus on close
- [ ] T052 [US7] `aria-label` on all icon-only buttons; no clickable `div` as button
- [ ] T053 [US7] Form label/error programmatic association (`aria-describedby`/`aria-invalid`) on auth + wallet forms
- [ ] T054 [US7] Contrast spot-checks: body text AA on zinc; gold for accents/large text; status colors vs. backgrounds
- [ ] T055 [US7] `prefers-reduced-motion`: verify shimmer/pulse/count-up respect the global rule

**Checkpoint**: keyboard walkthrough passes; audit clean.

---

## Phase 9: Cross-Cutting — UX Bug Backlog (T9)

**Purpose**: Fix backlog from `research.md` §3 (verify-after-build pattern — each item re-verified against its phase).

- [ ] T056 [X] Mobile sidebar rail in-flow (verify T017)
- [ ] T057 [X] Fake notification count / warn-red badge / dead `.env-staging` (verify T019)
- [ ] T058 [X] `setTimeout(800)` login navigation (verify T033)
- [ ] T059 [X] Registration dead-end success (verify T034)
- [ ] T060 [X] Dead Transfer/Withdraw buttons (verify T041)
- [ ] T061 [X] Mixed-currency `$` total (verify T040)
- [ ] T062 [X] Broken deposit-receipt tokens (verify T037)
- [ ] T063 [X] `empty-state` no-op action (verify T024)
- [ ] T064 [X] Dead `aegis-*` icons (verify T012/T030)
- [ ] T065 [X] Sweep for new defects (buttons without feedback, stale UI after mutations, missing success feedback, focus loss) — fix within scope

**Checkpoint**: all audit items closed; no new regressions.

---

## Phase 10: Polish (T10)

**Purpose**: Restrained final motion; fast, precise, intentional.

- [ ] T066 [POL] Tokenized transitions 150–200ms; hover elevations on cards; gold accent moments (active nav, focus, primary CTA)
- [ ] T067 [POL] Skeleton shimmer, toast slide-in, dialog/panel enter-exit, page-enter consistency
- [ ] T068 [POL] Zero layout shift on load (skeleton dims = content dims); final side-by-side vs. portfolio (same family, product density)

**Checkpoint**: polish pass clean; screenshots recorded.

---

## Phase 11: Evidence Refresh (T11) — feeds gate G8 (BLOCKS CLOSE)

**Goal**: No tracked UI evidence shows the old design.

- [ ] T069 [US8] Live stack up; e2e suite green on the new build
- [ ] T070 [US8] Re-capture `evidence/01–08*.png` with the SAME filenames and scenarios (login, wallets+premium, deposit form/filled, deposit receipt, 409 duplicate-reference, create wallet, two wallets) at 1440px dark theme
- [ ] T071 [US8] Regenerate `evidence/e2e/results.json` + `evidence/html-report/`
- [ ] T072 [US8] Update `e2e/README.md` captions where redesign changed what is shown (keep structure + Spanish)
- [ ] T073 [US8] Sweep docs/obsidian/READMEs for references to old-UI screenshots; confirm Kafka/load/observability evidence untouched

**Checkpoint (G8)**: side-by-side old vs. new — every UI screenshot shows the new design; references intact; no stale old-UI screenshots tracked in git.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 0 (Setup)**: no dependencies — starts immediately
- **Phase 1 (Foundational)**: depends on Phase 0 — **BLOCKS all user stories**
- **Phase 2 (US1, P1)**: after Phase 1 — the visual MVP
- **Phase 3 (US2, P1)**: after Phase 1; independent of US1 (parallelizable)
- **Phase 4 (US3, P2)**: after Phase 1; benefits from US2 components (loading-button, form-field-error)
- **Phase 5 (US4, P2)**: after Phase 1; depends on US2 components (stat-card, status-chip, empty-state, skeleton) and US1 shell
- **Phase 6 (US5, P3)**: after Phase 3 (empty-state modernized)
- **Phase 7 (US6, P2)**: after Phases 2 + 5 (shell + wallet grids); can run late
- **Phase 8 (US7, P3)**: after Phases 2–5 (interactive surface exists)
- **Phase 9 (Bug backlog)**: after Phases 2–5 (verify-after-build)
- **Phase 10 (Polish)**: after Phases 2–9
- **Phase 11 (Evidence)**: after Phase 10 — **G8 blocks close**

### Within Each User Story

- Update/extend Karma specs alongside implementation (write/update tests, ensure they pass with the change)
- Components before integrations; story complete before moving to next priority

### Parallel Opportunities

- T005–T008 (tokens) are [P]; T016/T018/T020, T022–T030 (components) are [P]
- US1 and US2 can run in parallel after Phase 1 if staffed

---

## Implementation Strategy

### MVP First

1. Phase 0 (baseline) → 2. Phase 1 (tokens/theme — critical) → 3. Phase 2 (US1 shell — visual MVP) → **STOP and validate** → demo-ready increment.

### Incremental Delivery

- Add US2 (components) → validate
- Add US3 (auth) → validate
- Add US4 (wallet) → validate
- Add US5–US8 (placeholders, responsive, a11y, backlog) → validate
- Polish (T10) → Evidence refresh (T11/G8) → Close

## Notes

- [P] = different files, no conflicts. Commit per task/logical group (`refactor(frontend):` / `fix(frontend):` per AGENTS.md).
- Each task group = one sub-issue under the epic (created at SDD step 5, Issues).
- Respect component style budget (6kB warn / 10kB error): move values into tokens/global.
- Stop at any checkpoint to validate independently.
