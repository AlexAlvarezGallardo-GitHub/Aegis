# Aegis Frontend — UI/UX Modernization Plan

**Scope:** `frontend/aegis-frontend` (Angular 22, Angular Material 22 M3, SCSS, Karma/Jasmine, ESLint)
**Reference:** `Aegis-Portfolio` (Astro + Tailwind, dark-only)
**Constraint set:** no backend/API changes, no new features, no rewrites, no new dependencies, preserve all functionality.
**Methodology:** **Specification-Driven Development (SDD)** — mandatory per `.specify/memory/constitution.md` §Specification-Driven Development. This document is the **SDD input** (audit + technical direction); execution happens through the speckit lifecycle and its artifacts (see §1).

---

## 0. Audit Findings (Rule 1 — Inspect First)

### Current state

| Area | Finding |
|---|---|
| Stack | Angular 22 standalone components, Angular Material M3 (`mat.define-theme`, density -2), lazy routes, signals |
| Real pages | **Login**, **Registration**, **Wallets** only. Default route → `/wallets` |
| Placeholders | 9 routes (`payments`, `transactions`, `payouts`, `currencies`, `fraud`, `alerts`, `health`, `settings`, `users`, `api-keys`) render `PagePlaceholderComponent` with hardcoded inline styles |
| Token system | Already exists: `styles/tokens/` (colors, typography, spacing, radius, shadows, transitions) + dark/light themes + mixins |
| Layout | `app-shell` (sidebar 240px/64px + header 56px + router-outlet), toasts, command palette, shortcut cheat-sheet |
| Shared components | loading-button, password-input, form-field-error, stat-card, status-chip, empty-state, loading-skeleton, toast-container, confirmation-dialog, command-palette, theme-toggle, aegis-icon, count-up directive |
| Backend | BFF on `:8082` via `proxy.conf.json`, relative URLs (correct per conventions), cookie-session auth (spec `010-bff`) |

### Portfolio visual language (extracted)

| Token | Portfolio value |
|---|---|
| Background | `#09090b` (zinc-950) |
| Surface | `#111113` |
| Card | `#18181b` (zinc-900) |
| Primary gold | `#d4af37` |
| Accent gold | `#e6c15a` |
| Foreground | `#fafafa` |
| Muted | `#a1a1aa` (zinc-400) |
| Borders | `rgb(255 255 255 / 0.06–0.10)` hairlines; hover → gold 40% |
| Fonts | Inter (sans), **Geist** (display/headings), JetBrains Mono (data) |
| Radius | 0.375rem–1rem |
| Focus ring | 2px solid gold, offset 2px |
| Selection | gold 30% alpha |
| Motion | restrained 150–200ms; `cubic-bezier(0.22,1,0.36,1)` for entrances |

### Problem checklist (file-referenced)

**Foundation**
1. Palette divergence: app uses navy `#0a0e1a` family + gold `#d4a843`; portfolio uses zinc `#09090b` + gold `#d4af37`. No Geist display font.
2. Undefined tokens referenced: `--aegis-surface-success` / `--aegis-border-success` used in `wallet.component.scss:463-464` but never defined → deposit receipt renders light-mint in dark mode.
3. Token bugs: gold `200`/`300` duplicate (`_colors.scss:28-29`); no z-index, breakpoint, icon-size, or focus-ring tokens (z-index `999/1000/1001/1100/1300` and breakpoints `480/768/1024` scattered).
4. Duplicate palette source: `themes/_dark.scss:28-46` re-declares navy/gold maps instead of importing `tokens/_colors.scss`.
5. Light theme incomplete: no component-level Material overrides, gold-600 on white fails contrast. **Decision: deprecate.**

**Layout / navigation**
6. Mobile sidebar bug: on phones the 64px collapsed rail stays in-flow; hamburger only toggles an expanded overlay (`app-shell.component.ts:73`, `sidebar.component.scss`).
7. Fake data: `notificationCount = signal(3)` (`header.component.ts:38`); badge uses Material `warn` red instead of gold; `.env-staging` CSS unreachable; breadcrumb is cosmetic (`segments[0]` capitalized).
8. Hardcoded px hotspots: header (43), wallet (38), sidebar (28).

**Shared components**
9. `loading-button`: `:focus-visible` only on primary variant; `transition: all`; hardcoded `#FFFFFF`.
10. `confirmation-dialog`: overlay `rgba(0,0,0,0.6)` hardcoded instead of `--aegis-surface-overlay`; no CDK focus trap.
11. `empty-state`: action button is a no-op when no route; hardcoded gold rgba border.
12. `status-chip` neutral variant hardcodes `rgba(100,116,139,0.12)`; toast icon uses spacing token as font-size.
13. `IconRegistryService.register()` never called → all custom `aegis-*` icons dead (404).
14. `page-placeholder` uses inline styles with literal px/hex.

**Auth**
15. Login navigates via `setTimeout(..., 800)` hack (`auth.component.ts:84`).
16. Registration success state is a dead end — no link back to login; no login link on the page at all.
17. `enableMockLogin: true` in dev makes AuthGuard auto-authenticate — real login flow unreachable in dev (flag for team; do not change behavior in this initiative).

**Wallets**
18. Premium badge hardcodes `#fbbf24/#d97706/#1a1a2e` gradient, bypassing gold tokens.
19. Balance sign colors hardcode `#16a34a/#dc2626` instead of semantic tokens.
20. `totalBalance` prepends `$` regardless of wallet currencies (mixed-currency aggregate is misleading).
21. Transfer/Withdraw buttons are dead no-ops (only View + Deposit work).

### Decisions (confirmed with stakeholder)

- **D1 — Full portfolio alignment:** re-point token values to zinc + gold `#d4af37` + Geist display font. Token *names* stay stable → component code barely changes.
- **D2 — Dark-only:** ship polished dark theme; remove theme toggle, light token mixins, and `_light.scss`. `data-theme="dark"` static.
- **D3 — Out of scope:** building Dashboard/Transactions/Settings features (they are placeholders today). The plan modernizes the placeholder pattern so future pages inherit the system. Auth mechanics (BFF cookie session, mock-login flag, no-op `http-auth.interceptor`) are **not** touched.

> D1–D3 are recorded as resolved **Clarifications** in `spec.md` during the Specify/Clarify steps (§1).

---

## 1. SDD Governance (constitution-mandated lifecycle)

This initiative is executed as **speckit feature `013-frontend-ui-modernization`**, scope `frontend-only`, following the full SDD cycle with its review gates (`.specify/workflows/speckit/workflow.yml`). No implementation begins before the spec, plan, and tasks gates are approved.

### 1.1 Lifecycle mapping

| # | SDD step | Command / agent | Artifact produced | Review gate | Fed by |
|---|----------|-----------------|-------------------|-------------|--------|
| 1 | **Specify** | `write-spec` skill → `/speckit.specify` (scope: `frontend-only`) | `specs/013-frontend-ui-modernization/spec.md` + branch `feature/013-frontend-ui-modernization` | **G1** spec review (scope, boundaries) | §0 audit + problem checklist + D1–D3 |
| 2 | **Clarify** | `/speckit.clarify` | `## Clarifications` section in spec.md | **G2** requirements review | open questions in §1.3 |
| 3 | **Plan** | `/speckit.plan` | `plan.md`, `research.md`, `quickstart.md` | **G3** technical design review | §2 phase plan of this document (seed) |
| 4 | **Tasks** | `/speckit.tasks` | `tasks.md` — dependency-ordered, checkpointed | **G4** task review | §2 phases → task groups T0–T10 |
| 5 | **Issues** | `/speckit.taskstoissues` + `issue-manager` | GitHub epic + per-phase sub-issues, linked task lists | **G5** issue review | tasks.md |
| 6 | **Analyze** | `/speckit.analyze` | cross-artifact consistency report | **G6** analysis review | spec + plan + tasks |
| 7 | **Checklist** | `spec-review` skill → `/speckit.checklist` | `checklists/*.md` quality checklists | **G7** checklist review | see §1.2 |
| 8 | **Implement** | `/speckit.implement` | working code, phase-by-phase | per-phase quality gates (§1.5) | tasks.md order |
| 9 | **Evidence review** | manual + Playwright re-capture (T11) | refreshed UI evidence (`evidence/*.png`, `e2e/README.md`, `html-report/`) | **G8 — evidence gate:** all UI-facing evidence reflects the new design; no stale screenshots of the old UI remain in the repo | inventory in §2 Phase 11 |
| 10 | **Close** | `/speckit.close` + `issue-manager` | closed epic, synced task lists | final verification (§3) — blocked until G8 passes | evidence |

### 1.2 Spec content requirements (what `/speckit.specify` must encode)

- **User stories** map 1:1 to user value, not to files: US-1 shell/navigation, US-2 shared components, US-3 authentication, US-4 wallets, US-5 placeholders, US-6 responsive, US-7 accessibility (each independently testable).
- **Functional requirements** are measurable and enforce the "no drift" rules, e.g.:
  - FR: all color/radius/shadow/spacing values MUST come from `--aegis-*` tokens; zero hex literals in component SCSS outside `styles/tokens/` (verified by grep in CI-quality gate).
  - FR: every referenced CSS custom property MUST be defined (zero `var(--undefined)` fallbacks).
  - FR: interactive components MUST expose hover / focus-visible / disabled / loading states where applicable.
  - FR: no horizontal overflow at 390px; financial figures use mono + `tabular-nums`.
  - FR: existing functionality MUST be preserved (auth flows, wallet CRUD/deposit, routing, guard behavior).
  - FR: all UI-facing evidence artifacts (`evidence/01–08*.png`, e2e HTML report, `e2e/README.md` captions) MUST be regenerated with the new design before close; non-UI evidence (Kafka, load, observability) is left untouched.
- **Success criteria:** lint/build/test green; e2e suite green; zero console errors; contrast spot-checks pass; bundle budgets respected.
- **Out of scope** section = §4 of this document verbatim.

### 1.3 Open questions routed to `/speckit.clarify`

1. Mixed-currency total balance: per-currency aggregate list vs. primary currency + "+n more"?
2. Transfer/Withdraw dead buttons: disabled with "Coming soon" tooltip vs. hidden?
3. Notification bell with fake count: hide entirely vs. keep with zero state?
4. Keep or delete `ThemeService`/`theme-toggle` code under dark-only (D2) — delete confirmed, verify no other consumers?
5. Mock-login default in dev: flag to team (no behavior change in this initiative).

### 1.4 Checklists (`spec-review` skill, adapted to `frontend-only` scope)

- Generate `checklists/testing.md` and `checklists/security.md` in full (both apply: Karma coverage conventions, XSS/aria/secrets rules).
- Backend checklists (`hexagonal.md`, `ddd.md`, `api.md`, `events.md`) are **N/A — no backend changes**; record the N/A rationale in the checklist file header instead of skipping silently.
- Add one UI-specific checklist `checklists/ui-consistency.md` (token usage, states coverage, portfolio alignment, responsive, a11y) — derived from §0 problem checklist and §3 final checklist.

### 1.5 Implementation governance

- **Branch:** created by the `speckit.git.feature` hook during Specify; one PR per phase (task group), squash-merged, `refactor(frontend):` / `fix(frontend):` commits, `Closes #<sub-issue>` footers; `issue-manager` syncs the epic checklist as sub-issues close.
- **Quality gate after EVERY task group** (blocking):
  1. `npm run lint` — clean
  2. `npm run build` — clean (watch `anyComponentStyle` budget: 6kB warn / 10kB error — keep component SCSS lean, push values into tokens)
  3. `npm test` (Karma ChromeHeadless) — green; update specs affected by structural changes
  4. Manual run against BFF sandbox (`:8082`) — affected screens, console error-free
  5. Responsive spot-check where relevant (1440 / 768 / 390)
  6. Evidence note appended to `evidence/unit/ui-modernization-unit.md` (scope, commands, pass/fail, screenshots)

---

## 2. Phase Plan

Each phase below is the seed for a **task group `T{n}` in `tasks.md`** (generated at SDD step 4) and a **sub-issue** under the epic (SDD step 5). Phases execute strictly in order; each ends with the quality gate from §1.5. The `**Gate**` line in each phase doubles as the tasks.md **checkpoint** for that group. T11 (evidence refresh) is the last group and feeds review gate **G8** before close.

### Phase 0 — Baseline & safety net (0.5 day) → `T0` (setup)

- [ ] Run app + capture screenshot inventory: login, register, wallets (populated/empty/loading/error), each placeholder route; at 1440/768/390.
- [ ] Record baseline: lint/build/test results, bundle sizes, existing spec failures (if any).
- [ ] Confirm dev sandbox (BFF `:8082`) reachable for manual verification.
- **Exit:** baseline evidence committed. (Epic + sub-issues already exist by this point — created at SDD step 5, before any implementation task group runs.)

### Phase 1 — Design tokens & theme foundation (1–2 days) → `T1` (foundational — blocks T2–T10)

**Goal:** one portfolio-aligned token layer; all later phases only consume tokens.

- [ ] **Colors** (`tokens/_colors.scss`): replace navy scale with zinc scale (`#fafafa…#09090b`, add 950); re-point gold scale around `#d4af37`/`#e6c15a`; keep every semantic alias name (`--aegis-color-bg`, `--aegis-surface-card`, `--aegis-border-default`, `--aegis-text-primary`, …) so consumers are untouched. Fix gold 200/300 duplicate; normalize hex casing; add `--aegis-surface-success` / `--aegis-border-success` (the missing tokens).
- [ ] **Typography** (`tokens/_typography.scss` + `index.html`): add Geist to the existing Google Fonts link (no new dependency); add `--aegis-font-display`; apply display font to `h1–h3`, `.kpi-value`, brand marks. Keep Inter body / JetBrains Mono data. Add `font-variant-numeric: tabular-nums` utility for financial figures.
- [ ] **New token categories:** `--aegis-z-*` (dropdown 1000 / overlay 1100 / drawer 1200 / modal 1300 / toast 1400 / palette 1500), `--aegis-breakpoint-*` (480/768/1024/1280 as SCSS vars + one exported TS constant consumed by `app-shell`), `--aegis-icon-size-*` (16/18/20/24/32), `--aegis-focus-ring` (2px gold, 2px offset).
- [ ] **Alias consolidation:** pick one naming scheme (`--aegis-*` semantic aliases), mark legacy duplicates deprecated, codemod consumers.
- [ ] **Theme files:** `_dark.scss` imports palette from `tokens/_colors.scss` (kill duplicate maps); hardcoded `#3B82F6`/`#EF4444`/`#FFFFFF` → token refs. Delete `_light.scss`, light color mixin, `theme-toggle` component, `ThemeService`; set `<html data-theme="dark">` statically in `index.html`; update/remove affected specs (`theme.service.spec.ts`, `theme-toggle` refs in header spec).
- [ ] **index.html:** `theme-color` → `#09090b`.
- [ ] **Icons:** register `IconRegistryService` via `APP_INITIALIZER` in `app.config.ts`; verify `aegis-*` SVGs render; keep Material Icons Outlined.
- [ ] **Global styles:** selection → gold 30%; focus-visible → ring token; scrollbar/borders to zinc hairlines.
- **Gate** + visual diff vs. baseline screenshots (expected: full palette shift, nothing broken).

### Phase 2 — Global layout & navigation (2 days) → `T2` (US-1)

- [ ] **app-shell/sidebar:** tokenize all dims (240px/64px rail/56px header → layout tokens); zinc surfaces + white/6% hairlines; **fix mobile rail bug** — sidebar hidden off-canvas on mobile, opens as overlay drawer with backdrop + body scroll-lock; close on route change & Escape.
- [ ] **Nav items:** active state = gold left-pill + subtle gold-50 bg (keep pattern, re-point colors); hover/focus-visible states; collapsed-rail tooltips retained; section labels to token typography.
- [ ] **Header:** remove fake `notificationCount` (hide bell until a real feed exists — no feature addition); if kept visible, badge → gold not warn-red; breadcrumb from route `data.title` (already declared in routes) instead of string capitalization; remove dead `.env-staging` CSS or wire env properly; user menu styled to tokens.
- [ ] **Page container:** consistent max-width + spacing scale; keep `aegis-page-enter` animation.
- **Gate** + responsive smoke at 1440/768/390 (drawer behavior verified on touch viewport).

### Phase 3 — Shared components (2–3 days) → `T3` (US-2)

Harden the component inventory so every page inherits consistency. For each: hover / focus-visible / disabled / loading / error / success where applicable, all values tokenized.

- [ ] `loading-button`: focus ring on **all** variants; remove `transition: all` (explicit properties); `#FFFFFF` → `--aegis-color-on-primary`; spinner size token.
- [ ] `confirmation-dialog`: overlay → `--aegis-surface-overlay`; destructive label token; add CDK `A11yModule` focus trap.
- [ ] `empty-state`: rgba border → gold-100 token; hide action button when no handler (fixes no-op click).
- [ ] `status-chip`: neutral variant → semantic tokens.
- [ ] `stat-card`: variant gradients → tokenized gold/semantic tints; value → display font.
- [ ] `toast-container`: icon size via text token.
- [ ] `loading-skeleton`: shimmer tuned for zinc surfaces.
- [ ] `page-placeholder`: rewrite with tokens, reuse `empty-state` (kills inline styles).
- [ ] `password-input` / `form-field-error`: verify token usage, add `autocomplete` hints (see Phase 4).
- [ ] Add/extend specs for state behavior (disabled, loading transitions) per frontend testing conventions.
- **Gate** + component-level visual check.

### Phase 4 — Authentication (1–2 days) → `T4` (US-3)

- [ ] **Layout:** premium centered auth screen — zinc background with the portfolio's subtle grid + soft gold glow (restrained, app-appropriate), brand mark, card on `--aegis-surface-card` with hairline border. Same shell for login + registration.
- [ ] **Login:** remove `setTimeout(800)` — navigate immediately on success, toast persists across navigation; keep `returnUrl`; `autocomplete="email/current-password"`; autofocus email; Enter submits.
- [ ] **Registration:** success state gains primary "Continue to sign in" action; add "Already have an account? Sign in" link (fixes one-way funnel); `autocomplete="new-password"` + name fields.
- [ ] **Errors:** keep field-level `form-field-error` + toast pattern; ensure server error messages map to friendly copy; no layout shift when errors appear.
- [ ] **Do not touch:** auth service logic, guard, mock-login flag, interceptors.
- **Gate** + full manual flow: register → login → wallets → logout → guard redirect; invalid-submit toast; loading button disabled states.

### Phase 5 — Wallets (2–3 days) → `T5` (US-4)

The only real data page — gets the deepest treatment (absorbs "Dashboard" phase: wallets *is* the landing page).

- [ ] **Fix broken tokens:** deposit receipt → `--aegis-color-success-bg/-text` (currently light-mint in dark).
- [ ] **Premium badge** → gold token gradient (`--aegis-gold-400→600`), text → `--aegis-color-on-primary`.
- [ ] **Balance signs** → `--aegis-color-success/-error` tokens; amounts in JetBrains Mono + `tabular-nums`.
- [ ] **Currency correctness:** kill unconditional `$` prefix on `totalBalance` — aggregate per currency and render each (or primary currency + "+n more"); introduce one shared currency-format pipe/util reused by cards, KPIs, and receipt.
- [ ] **Wallet cards:** hierarchy — currency + status chip top, scannable balance center, ID/date muted footer, actions row right-aligned; hover elevation restrained; PREMIUM as chip not gradient blob.
- [ ] **Actions:** View + Deposit stay functional; Transfer/Withdraw → `disabled` with tooltip "Coming soon" (honest UI, no feature change).
- [ ] **Slide-over panels** (create/deposit): tokenized surfaces, focus trap, Escape + backdrop close, full-width ≤480px, idempotency receipt restyled with semantic tokens.
- [ ] **States:** skeleton (6 cards) aligned to zinc; empty state uses modernized `empty-state`; error state with retry action (re-fetch).
- **Gate** + manual deposit flow end-to-end (409 duplicate-reference toast verified).

### Phase 6 — Placeholder routes (0.5 day) → `T6` (US-5)

- [ ] All 9 placeholder routes render the modernized placeholder (icon, route title from `data.title`, "In development" copy, token styling).
- **Gate** — quick visual pass of every route.

### Phase 7 — Responsive design (1 day) → `T7` (US-6)

- [ ] Replace scattered breakpoints with tokens; audit at 1440 / 1280 / 1024 / 768 / 480 / 390.
- [ ] Verify: mobile drawer (Phase 2), header condensation, wallet KPI grid 4→2→1, card grid auto-fill, slide panels full-screen on mobile, auth card padding, command palette width.
- [ ] No horizontal overflow anywhere; long balances / wallet IDs truncate with ellipsis + `title` attr; toasts fit small viewports.
- **Gate** + screenshot set per breakpoint.

### Phase 8 — Accessibility (1 day) → `T8` (US-7)

- [ ] Gold focus ring token applied globally; verify every interactive element keyboard-reachable (nav, cards' actions, panels, dialogs, palette).
- [ ] Skip-to-content link; semantic `<button>` audit (no clickable divs); `aria-label` on all icon-only buttons; dialog/panel focus traps; `aria-live` toasts (exists — verify).
- [ ] Form labels + error association (`aria-describedby`) on auth forms.
- [ ] Contrast spot-check: gold on zinc for large text/icons/accents; body text ≥ zinc-300; status colors vs. backgrounds.
- [ ] `prefers-reduced-motion` honored (global rule exists — verify shimmer/pulse/count-up respect it).
- **Gate** + keyboard-only walkthrough of login → wallets → deposit.

### Phase 9 — UX bug backlog (1 day) → `T9` (cross-cutting fixes; verify-after-build pattern: each item re-verified against its earlier phase)

Fix, from the audit (each file-referenced in §0):

1. Mobile sidebar rail in-flow (Phase 2 delivers; verify here).
2. Fake notification count / warn-red badge / dead env-staging (Phase 2; verify).
3. `setTimeout(800)` login navigation (Phase 4; verify).
4. Registration dead-end success (Phase 4; verify).
5. Dead Transfer/Withdraw buttons (Phase 5; verify).
6. Mixed-currency `$` total (Phase 5; verify).
7. Broken deposit-receipt tokens (Phase 5; verify).
8. `empty-state` no-op action (Phase 3; verify).
9. Dead `aegis-*` icons → registry initialized (Phase 1; verify).
10. Any new defects found during Phases 1–8 (buttons without feedback, stale UI after mutations, missing success feedback, focus loss).
- **Out of scope, flagged for team:** `enableMockLogin: true` default in dev; no-op `http-auth.interceptor` (believed intentional under BFF cookie sessions — confirm with backend owner).

### Phase 10 — Final polish (0.5–1 day) → `T10`

- [ ] Restrained transitions everywhere (150–200ms, tokenized); card hover elevation; gold accent moments (active nav, focus, primary CTA) — never decorative noise.
- [ ] Skeleton shimmer, toast slide-in, dialog/panel enter-exit, page-enter animation consistency.
- [ ] Zero layout shift on load (skeleton dims match content dims).
- [ ] Final side-by-side vs. portfolio: same family, app-appropriate density.

### Phase 11 — Evidence refresh (0.5 day) → `T11` (final task group; feeds gate **G8**)

**Goal:** every UI-facing evidence artifact in the repo reflects the new design. Triggered after T10, executed against the live stack, reviewed at gate **G8** before `/speckit.close`.

Current inventory (audited) and disposition:

| Artifact | Contains old UI? | Action |
|---|---|---|
| `evidence/01-login-filled.png` … `08-two-wallets.png` (8 screenshots: login, wallets, deposit form/receipt, 409 idempotency, create-wallet) | **Yes** | **Re-capture** with the new design via Playwright against the live stack (`infra/docker-compose.yml` up, frontend on `:4200`, registered user per `e2e/README.md`) — same flows, same filenames, so all existing references stay valid |
| `evidence/e2e/results.json`, `evidence/html-report/` | Regenerable run artifacts | Regenerate with a full green e2e run on the new build |
| `e2e/README.md` evidence table (captions in Spanish) | Captions describe UI elements (e.g. "badge PREMIUM") | Update captions only where the redesign changed what the screenshot shows |
| `evidence/09–12` Kafka screenshots | No (Kafka UI) | Untouched |
| `evidence/load*/` (RESULTS.md + raw outputs) | No (backend perf) | Untouched |
| `evidence/observability/` (Grafana, traces) | No | Untouched |
| Docs referencing evidence (`docs/project-status.md`, `docs/postmortems/001`, obsidian) | Reference load/observability evidence only | Verify links still valid; no visual refresh needed |

- [ ] Live stack up (`docker compose -f infra/docker-compose.yml up -d`) + e2e suite green on the new build.
- [ ] Re-capture `01–08` screenshots: same scenarios and filenames, new design, consistent viewport (1440px) and dark theme.
- [ ] Regenerate `evidence/e2e/results.json` + `evidence/html-report/`.
- [ ] Update `e2e/README.md` captions where redesign changed depicted UI; keep table structure and language (Spanish).
- [ ] Sweep for other references to UI screenshots (docs, obsidian vault, READMEs) and confirm none display the old design.
- **Gate (G8):** side-by-side review old vs. new captures — every UI screenshot shows the new design, filenames/references intact, e2e report green. No stale old-UI evidence remains tracked in git.

---

## 3. Final verification (blocking)

- [ ] `npm run lint` clean
- [ ] `npm run build` clean, budgets respected
- [ ] `npm test` green (all specs, incl. updated theme/header/auth/wallet specs)
- [ ] E2E suite (`e2e/` Playwright) green against running sandbox — **no regressions in auth/wallet flows**
- [ ] Console error-free on all routes
- [ ] No horizontal overflow at 390px
- [ ] Evidence written: `evidence/unit/ui-modernization-unit.md` (commands, results, screenshots per phase)
- [ ] All `checklists/*.md` items signed off (SDD step 7 artifacts)
- [ ] **G8 passed:** UI evidence regenerated with the new design (`evidence/01–08*.png`, `results.json`, `html-report/`), `e2e/README.md` captions updated, zero stale old-UI screenshots tracked in git
- [ ] `/speckit.analyze` re-run clean on final artifacts (spec ↔ plan ↔ tasks ↔ code consistency)
- [ ] `/speckit.close` executed: `issue-manager` verifies all sub-issues closed, syncs epic task list, closes epic; PR merged to `main` with all CI checks green

### Final checklist mapping

| Brief requirement | Delivered by |
|---|---|
| Consistent Aegis branding / portfolio quality | Phase 1 (zinc + gold + Geist) |
| Consistent typography/spacing/colors/components | Phases 1, 3 |
| No generic admin-template look | Phases 2, 4, 5 (custom shell, branded auth, fintech wallet cards) |
| Clear navigation/actions/loading/empty/error/success | Phases 2, 3, 5 |
| Financial info easy to scan | Phase 5 (mono + tabular-nums + currency fix) |
| Responsive, no overflow | Phase 7 |
| Keyboard/focus/labels/contrast/semantic | Phase 8 |
| Reusable components, centralized tokens, no new deps | Phases 1, 3 (Geist via existing Google Fonts link) |
| No broken functionality, build passes | Every phase gate + final verification |

**Estimated total effort:** 11–15 working days.

---

## 4. Explicit non-goals

- Building Dashboard/Transactions/Settings/Payments features (placeholders stay placeholders).
- Backend, BFF, API contract, proxy, or environment changes.
- Auth/session mechanics (guard, interceptors, mock-login flag).
- New npm dependencies or UI libraries (Angular Material stays; Geist loads via the existing Google Fonts `<link>`).
- Light theme (deprecated per D2).
