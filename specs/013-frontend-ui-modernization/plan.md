# Implementation Plan: UC-013 Frontend UI/UX Modernization

**Branch**: `feature/013-frontend-ui-modernization` | **Date**: 2026-08-09 | **Spec**: [`spec.md`](./spec.md)

**Input**: Feature specification from `specs/013-frontend-ui-modernization/spec.md` + research from `research.md`.

## Summary

Modernize the Aegis Angular frontend (`frontend/aegis-frontend`, Angular 22 + Material M3) to the portfolio's visual language (**zinc surfaces + gold `#d4af37` accent + Geist display font, dark-only**) while preserving all functionality. Approach: **re-point the existing design-token system** (values change, names stay stable) rather than rewriting; then modernize shell/navigation, harden shared components, and treat auth + wallets as the two flagship surfaces. Nine placeholder routes inherit the modernized placeholder pattern. UI-facing evidence is regenerated before close (gate G8).

Non-goals (per spec): no backend/BFF/API/proxy changes; no new npm dependencies; no new feature pages; light theme removed (D2).

## Technical Context

**Language/Version**: TypeScript ~6.0, Angular 22 (standalone components, signals), SCSS

**Primary Dependencies**: Angular Material 22 (M3 `mat.define-theme`), Angular CDK, `@angular/forms/router/animations`, rxjs 7.8. **No additions** — Geist loads via the existing Google Fonts `<link>` in `index.html`.

**Storage**: None for this feature. `localStorage` already used only for `aegis-sidebar-collapsed` (kept). Session auth = BFF HttpOnly cookie (untouched).

**Testing**: Karma/Jasmine unit (`npm test`), Playwright e2e (`e2e/`, green against live stack), ESLint (`npm run lint`), build (`npm run build`). Test tiers/evidence per `AGENTS.md`.

**Target Platform**: Modern browsers (Chromium, Firefox, Safari); responsive 1440 → 390px.

**Project Type**: SPA web application.

**Performance Goals**: Bundle budgets respected (initial 600kB warn / 1.2MB error; `anyComponentStyle` 6kB warn / 10kB error); zero console errors; no layout shift on load (skeletons match content); transitions ≤200ms.

**Constraints**: token-only styling (zero hex in component SCSS outside `tokens/`); zero undefined `var()` references; no hardcoded px where tokens exist; budgets respected; existing behavior preserved (routes, guards, interceptors, proxy).

**Scale/Scope**: 11 task groups (T0–T11); 3 real pages + 10 placeholder routes; ~20 shared components; 5 token files + 2 theme files.

## Constitution Check

*GATE: pass before T0; re-check after T1.*

| Constitution gate | Status | Notes |
|-------------------|--------|-------|
| **Principle I — Hexagonal Architecture** | N/A | Frontend-only; no backend/domain code changed |
| **Principle II — Domain Ownership** | N/A | No service boundaries or data ownership affected |
| **Principle III — Event-Driven** | N/A | No Kafka/event changes |
| **Principle IV — Security-First** | **PASS** | No secrets added; `post-edit-security` plugin active; auth mechanics untouched; a11y/contrast checks in T8 |
| **Principle V — Test-Driven Quality** | **PASS** | Karma specs updated with each structural change; e2e green per gate; evidence report per phase (`evidence/unit/ui-modernization-unit.md`); gate G8 for evidence refresh |
| **SDD lifecycle** | **PASS** | This plan is SDD step 3; tasks (step 4), issues (step 5), analyze (step 6), checklist (step 7), implement (step 8), close (step 10) |

## Project Structure

### Documentation (this feature)

```text
specs/013-frontend-ui-modernization/
├── spec.md            # SDD step 2 (approved G1, clarified G2)
├── research.md        # SDD step 3 — portfolio extraction + audit (Phase 0)
├── plan.md            # SDD step 3 — this file (Phase 1)
├── quickstart.md      # SDD step 3 — run/verify guide (Phase 1)
├── contracts/         # NOT CREATED — no API/event contract changes (frontend-only)
├── data-model.md      # NOT CREATED — no data model; the "design model" (tokens/theme/components) is documented in plan.md §Design model
└── tasks.md           # SDD step 4 (/speckit.tasks) — NOT created here
```

### Source Code (repository root)

```text
frontend/aegis-frontend/
├── src/
│   ├── index.html                        # Geist link, theme-color #09090b, data-theme="dark"
│   ├── styles.scss                       # global: focus ring, selection, reduced-motion
│   ├── theme.scss                        # dark-only M3 overrides
│   ├── styles/
│   │   ├── tokens/                       # re-pointed values + new categories
│   │   │   ├── _colors.scss              #   zinc + gold #d4af37; + surface-success/border-success
│   │   │   ├── _typography.scss          #   + --aegis-font-display (Geist)
│   │   │   ├── _spacing.scss             #   unchanged scale
│   │   │   ├── _radius.scss              #   unchanged scale
│   │   │   ├── _shadows.scss             #   tuned for zinc
│   │   │   ├── _transitions.scss         #   + durations/easing
│   │   │   └── _index.scss               #   + new categories (z-index, breakpoints, icons, focus)
│   │   ├── themes/
│   │   │   ├── _index.scss               #   dark-only
│   │   │   └── _dark.scss                #   imports palette from tokens (single source of truth)
│   │   └── mixins/                       #   interactions/forms: focus-ring mixin
│   └── app/
│       ├── app.config.ts                 # + IconRegistryService APP_INITIALIZER
│       ├── app.routes.ts                 # unchanged (route data.title drives breadcrumb)
│       ├── features/
│       │   ├── auth/                     # branded login; remove setTimeout(800)
│       │   ├── registration/             # + sign-in link; success action → login
│       │   └── wallet/                   # receipt tokens, currency pipe, disabled actions
│       └── shared/
│           ├── components/               # theme-toggle REMOVED; rest hardened
│           ├── data-display/             # stat-card/status-chip/empty-state/skeleton → tokens
│           ├── forms/                    # loading-button focus-all-variants
│           ├── layout/                   # app-shell/sidebar/header/page-placeholder modernized
│           ├── icons/                    # IconRegistryService wired (register() called)
│           └── services/                 # ThemeService REMOVED; toast kept
```

**Structure Decision**: keep the existing standalone-component architecture and directory layout; no structural reorg. Changes are confined to token values, shell/components styling, and two flagships (auth, wallet). Shared `currency` format moved to `shared/utils/currency.pipe.ts` (new small util, not a new dependency).

## Design model (token/theme/component strategy)

### 1. Design tokens (T1)

- Re-point color/typography values per `research.md` §4 (zinc + gold `#d4af37`/`#e6c15a` + Geist display).
- Add missing categories: z-index, breakpoints, icon sizes, focus ring; define `--aegis-surface-success`/`--aegis-border-success`.
- Fix token bugs (duplicate gold 200/300, casing). Consolidate legacy aliases to one `--aegis-*` scheme.
- Keep token names → component code unchanged (only SCSS values move).

### 2. Theming (T1)

- Dark-only: `_dark.scss` imports palette from `tokens/_colors.scss` (single source of truth); hardcoded M3 vars → token refs.
- Delete `_light.scss`, light color mixin, `ThemeService`, `theme-toggle`; static `<html data-theme="dark">`; `theme-color` → `#09090b`.
- Verify zero remaining consumers (grep `ThemeService`, `data-theme`, `theme-toggle`) and update affected specs.

### 3. Component hardening (T3)

State matrix (hover / focus-visible / disabled / loading / error / success) per component, all token-driven:
`loading-button`, `password-input`, `form-field-error`, `stat-card`, `status-chip`, `empty-state`, `loading-skeleton`, `toast-container`, `confirmation-dialog` (CDK focus trap), `command-palette`. Placeholder page reuses `empty-state` (kills inline styles).

### 4. Shell & navigation (T2)

- Sidebar off-canvas on mobile + overlay drawer (backdrop, Escape, route-close, scroll-lock) — fixes in-flow rail bug.
- Header: remove fake notification bell/badge (A9); breadcrumb from route `data.title`; tokenized dims.
- Active nav gold-pill state; focus-visible everywhere; page-container max-width + spacing.

### 5. Authentication (T4)

- Branded centered layout (zinc bg + subtle grid/glow); login navigates immediately on success (toast persists) — no `setTimeout`.
- Registration: "Already have an account? Sign in" link; success state → "Continue to sign in".
- `autocomplete` hints, autofocus, label/error association (ARIA).

### 6. Wallets (T5)

- Fix deposit receipt tokens (success surface/border) — the flagship bug.
- Premium badge → gold tokens; balance signs → semantic tokens; mono + `tabular-nums`.
- Per-currency aggregate total via shared `currency.pipe.ts` (A7); Transfer/Withdraw disabled + "Coming soon" tooltip (A8).
- Slide-over panels: tokenized, focus trap, full-width ≤480px. States: skeleton/empty/error+retry.

### 7. Placeholders / responsive / a11y / bugs / polish (T6–T10)

- T6: tokenized placeholder pattern on all 9 routes.
- T7: breakpoint tokens; verify 1440/1280/1024/768/480/390; no horizontal overflow.
- T8: skip-link, keyboard, focus rings, ARIA labels, semantic buttons, contrast, reduced-motion.
- T9: bug backlog from `research.md` §3 (verify-after-build).
- T10: restrained motion (150–200ms), hover elevations, no layout shift.

### 8. Evidence (T11, gate G8)

Re-capture `evidence/01–08*.png` with the same filenames/scenarios on the new build; regenerate `e2e/results.json` + `html-report/`; update `e2e/README.md` captions; leave Kafka/load/observability untouched; sweep for stale old-UI references.

## Implementation order (tasks groups)

| Group | Phase | Scope | Gate |
|-------|-------|-------|------|
| T0 | Baseline & safety net | Screenshot inventory, baseline lint/build/test, live-stack check | G-quality |
| T1 | Design tokens & theme | Tokens, dark-only, fonts, icon registry, alias codemod | G-quality + Constitution re-check |
| T2 | Shell & navigation | app-shell/sidebar/header | G-quality + responsive smoke |
| T3 | Shared components | component hardening + specs | G-quality |
| T4 | Authentication | login/registration UX | G-quality + full flow |
| T5 | Wallets | cards/KPIs/currency/panels/receipt | G-quality + deposit E2E |
| T6 | Placeholder routes | tokenized placeholder | G-quality |
| T7 | Responsive | breakpoint audit, no overflow | G-quality + viewport matrix |
| T8 | Accessibility | keyboard/focus/ARIA/contrast | G-quality + keyboard walkthrough |
| T9 | UX bug backlog | audit fixes verify-after-build | G-quality |
| T10 | Polish | motion, hover, CLS | G-quality |
| T11 | Evidence refresh | re-capture + regenerate | **G8** (blocks close) |

Per-group quality gate (blocking): `npm run lint` → `npm run build` → `npm test` (updated specs) → manual run against BFF sandbox (`:8082`) → responsive spot-check → evidence note in `evidence/unit/ui-modernization-unit.md`.

## Complexity Tracking

No constitution violations. No complexity justification required. (No new projects, repositories, or architecture layers are introduced.)

## References

- Strategy plan: `frontend/aegis-frontend/UI-MODERNIZATION-PLAN.md`
- Constitution: `.specify/memory/constitution.md` (§Specification-Driven Development, Principles IV–V)
- Test tiers & evidence: `AGENTS.md`
