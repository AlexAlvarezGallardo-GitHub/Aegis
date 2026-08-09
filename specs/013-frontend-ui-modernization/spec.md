# Feature Specification: UC-013 Frontend UI/UX Modernization

**Feature Branch**: `feature/013-frontend-ui-modernization`

**Created**: 2026-08-09

**Status**: Draft

**Input**: User description: "Modernize the Aegis Angular frontend to reach the same visual quality, consistency and polish as the Aegis portfolio (Aegis-Portfolio), while preserving all existing functionality. Do not redesign the product, do not change backend logic or APIs. Scope is frontend-only. Execution follows Specification-Driven Development."

**Source strategy document**: `frontend/aegis-frontend/UI-MODERNIZATION-PLAN.md` (audit findings, decisions D1–D3, phase plan T0–T11, review gates G1–G8).

---

## Problem

The Aegis frontend (`frontend/aegis-frontend`, Angular 22 + Angular Material 22 M3) does not yet belong visually to the Aegis ecosystem. The audit identified:

- **Palette divergence:** the app uses a dark navy palette (`#0a0e1a` family, gold `#d4a843`) while the portfolio uses zinc near-black (`#09090b/#111113/#18181b`) with gold `#d4af37`; no Geist display font.
- **Token-system defects:** undefined CSS custom properties are referenced (`--aegis-surface-success`, `--aegis-border-success`) breaking the deposit receipt in dark mode; duplicate gold steps; no z-index/breakpoint/icon-size/focus-ring tokens.
- **Layout defects:** mobile sidebar rail stays in-flow on phones; fake hardcoded notification count; Material `warn`-red badge; cosmetic breadcrumb; hardcoded px/hex hotspots (header 43, wallet 38, sidebar 28).
- **Component gaps:** missing `:focus-visible` on outline/ghost buttons; hardcoded `#FFFFFF`/rgba values; dead `aegis-*` icons (registry never registered); unstyled placeholder pages with inline styles.
- **Auth UX defects:** `setTimeout(800)` navigation hack; registration success is a dead end (no link to login).
- **Wallet UX defects:** broken deposit-receipt tokens; hardcoded premium-badge gradient; sign colors bypass semantic tokens; unconditional `$` on mixed-currency total; dead Transfer/Withdraw buttons.
- **Evidence staleness:** `evidence/01–08*.png` UI screenshots show the old design.

## Solution

A frontend-only modernization executed under SDD that:

1. Adopts the portfolio visual language through the **existing design-token architecture** (token *names* stay stable; only values change), plus Geist as display font and a dark-only theme.
2. Modernizes the application shell, shared components, authentication, and the wallet experience.
3. Fixes the enumerated UX/token/accessibility/responsive defects without changing business logic, routes, API contracts, or adding dependencies.
4. Regenerates UI-facing evidence with the new design before close.

The app should feel like a modern, premium fintech/enterprise platform that belongs to the same design ecosystem as the Aegis portfolio, while keeping **product density and usability** (portfolio = brand; application = product).

---

## User Scenarios & Testing

User stories are prioritized user journeys, each independently testable as an MVP slice. They map to task groups T2–T11 of the strategy plan (T1, the design-token foundation, is a cross-cutting prerequisite defined under Requirements, not a user story). Gaps, ambiguities and [NEEDS CLARIFICATION] items are routed to `/speckit.clarify`.

### User Story 1 - Application Shell & Navigation (Priority: P1)

As a user of the platform, I want the app shell, sidebar, header and navigation to look and behave like a modern premium product, so that the platform feels credible and consistent from the first page.

**Why this priority**: The shell is the frame for every page — its visual state defines first impression and carries all navigation. No page can feel modern while the shell does not.

**Independent Test**: Can be fully tested by opening any authenticated page and verifying shell layout, active-navigation states, mobile drawer behavior, and that navigation between all routes still works.

**Acceptance Scenarios**:

1. **Given** an authenticated user on desktop, **when** the app loads any route, **then** the shell renders zinc-toned surfaces with hairline borders, tokenized dimensions, and a gold active-nav indicator; no generic admin-template appearance.
2. **Given** an authenticated user on a phone viewport (≤768px), **when** the app loads, **then** the sidebar is hidden off-canvas (no in-flow 64px rail); the header hamburger opens it as an overlay drawer with backdrop, Escape/backdrop/route-change close it, and body scroll is locked while open.
3. **Given** the user navigates, **when** a nav item becomes active, **then** it shows the gold active state and each nav item is keyboard-focusable with a visible gold focus ring.
4. **Given** the header, **when** rendered, **then** it shows no fabricated notification count (bell hidden until a real feed exists), no Material `warn`-red badge, and the breadcrumb reflects the route `data.title`.
5. **Given** any sidebar/header dimension, **when** inspected, **then** values come from tokens (no hardcoded px in layout SCSS outside token files).
6. **Given** light-theme artifacts, **when** the app loads, **then** only the dark theme is active (per decision D2).

### User Story 2 - Shared Component Library (Priority: P1)

As a frontend engineer, I want a consistent, hardened set of shared components, so that every page inherits the same visual behavior without duplicated markup or styles.

**Why this priority**: Components are the building blocks of every future page; consistency here is the cheapest place to enforce quality. A mediocre component used consistently beats five divergent implementations.

**Independent Test**: Can be fully tested by exercising the shared components on the existing pages (auth forms, wallet cards, toasts, dialogs, empty/skeleton states) and verifying each has hover/focus-visible/disabled/loading states where applicable.

**Acceptance Scenarios**:

1. **Given** any interactive component (buttons, inputs, selects, toggles, icon buttons), **when** focused via keyboard, **then** a visible gold focus ring appears (including outline/ghost variants).
2. **Given** the confirmation dialog, **when** opened, **then** focus is trapped, overlay uses `--aegis-surface-overlay`, destructive labels use semantic tokens, and Escape/overlay-click cancel.
3. **Given** the empty-state component, **when** no action handler is provided, **then** the action button is not rendered (no dead click).
4. **Given** status-chip/stat-card/toast/loading-skeleton, **when** inspected, **then** all colors and radii come from tokens; no hex literals or `rgba()` outside `styles/tokens/`.
5. **Given** the custom `aegis-*` icons, **when** the app loads, **then** `IconRegistryService.register()` has run (icons render, no console 404s).
6. **Given** the placeholder pages, **when** rendered, **then** they use the tokenized placeholder/empty-state pattern (no inline styles with literal px/hex).

### User Story 3 - Authentication Experience (Priority: P2)

As an unauthenticated visitor, I want login and registration to feel secure, modern, premium and simple, so that joining and returning to the platform is frictionless.

**Why this priority**: Auth is the first screen new and returning users see; brand trust starts there.

**Independent Test**: Can be fully tested by registering a user and logging in against the live BFF stack, verifying validation feedback, loading states, success navigation, and error handling.

**Acceptance Scenarios**:

1. **Given** the login page on the new design, **when** rendered, **then** it shows the branded Aegis treatment (zinc background, restrained gold glow/grid, card on token surface) and is responsive on mobile.
2. **Given** a user submits valid credentials, **when** authentication succeeds, **then** navigation to `returnUrl` (default `/wallets`) happens immediately — **no `setTimeout` delay** — and success feedback is shown via toast; loading button stays disabled until the request resolves and resets via `finalize` on success, error and timeout.
3. **Given** invalid form input, **when** the user submits, **then** field-level errors and an invalid-submit toast appear; no layout shift when errors render.
4. **Given** the registration page, **when** registration succeeds, **then** the success state offers a primary "Continue to sign in" action, and the page includes an "Already have an account? Sign in" link.
5. **Given** the forms, **when** inspected, **then** `autocomplete` hints are set, first field is autofocused, and labels are associated with controls (`aria-describedby`).
6. **Given** the mock-login and interceptor behavior, **when** the auth flows run, **then** auth mechanics are unchanged (out of scope: guard, mock-login flag, interceptors).

### User Story 4 - Wallet Experience (Priority: P2)

As a wallet user, I want the wallet landing page (the default authenticated page) to present balances, currencies, statuses and actions so that financial information is easy to scan and deliberate to act on.

**Why this priority**: Wallets is the only real data page and the default post-login destination — the product's centerpiece.

**Independent Test**: Can be fully tested against the live BFF stack by creating a wallet, viewing the list, and performing a deposit end-to-end (including the 409 duplicate-reference path).

**Acceptance Scenarios**:

1. **Given** the wallet list, **when** rendered, **then** each card shows currency + status chip, a scannable mono/tabular-nums balance, muted wallet ID/date, and right-aligned actions; KPI stat-cards use the display font and gold/semantic token tints.
2. **Given** a wallet with a positive or negative-ish balance, **when** rendered, **then** sign colors use `--aegis-color-success`/`--aegis-color-error` tokens (no hardcoded hex).
3. **Given** the total-balance summary across mixed currencies, **when** rendered, **then** it does not mislabel amounts with a single `$`; it renders a **per-currency aggregate** (e.g. `€ 150.00 · $ 200.00`) via one shared currency-formatting pipe (decision **A7**).
4. **Given** the deposit flow, **when** a deposit succeeds, **then** the receipt uses defined semantic tokens (`--aegis-color-success-bg/-text`) and renders correctly in dark mode (no light-mint block); idempotency (409) still shows the duplicate-reference toast.
5. **Given** Transfer/Withdraw actions, **when** rendered, **then** they remain visible but disabled with a "Coming soon" tooltip rather than dead clicks (decision **A8**).
6. **Given** loading/empty/error states, **when** the page loads, **then** skeletons match content dimensions (no layout shift), the empty state uses the shared component, and an error state offers a retry action.

### User Story 5 - Placeholder Pages Consistency (Priority: P3)

As a user visiting not-yet-built sections, I want consistent, branded placeholders so that the product feels finished and navigable even where features are pending.

**Why this priority**: 9 routes are placeholders today; consistency prevents a jarring half-finished feel.

**Independent Test**: Can be fully tested by visiting each placeholder route and verifying the shared tokenized placeholder pattern (icon, route title, "In development" copy).

**Acceptance Scenarios**:

1. **Given** any placeholder route (`payments`, `transactions`, `payouts`, `currencies`, `fraud`, `alerts`, `health`, `settings`, `users`, `api-keys`), **when** rendered, **then** it shows the tokenized placeholder/empty-state pattern with the route title from `data.title`.
2. **Given** the placeholder component, **when** inspected, **then** no inline styles with literal px/hex remain.

### User Story 6 - Responsive Behavior (Priority: P2)

As a user on any device, I want every screen to lay out intentionally at desktop, laptop, tablet and mobile, so that no content overflows and interactions remain usable.

**Why this priority**: Broken responsive behavior erodes trust on mobile; the app must not simply shrink desktop layouts.

**Independent Test**: Can be fully tested by walking the main flows at 1440/1280/1024/768/480/390 and verifying no horizontal overflow and usable interactions.

**Acceptance Scenarios**:

1. **Given** any viewport, **when** inspected, **then** there is no horizontal overflow; long balances and wallet IDs truncate with ellipsis and `title` attribute.
2. **Given** a phone viewport, **when** the KPI grid, wallet card grid, slide-over panels, auth card and command palette render, **then** each uses an intentional mobile layout (grids collapse 4→2→1, panels go full-width, forms fit).
3. **Given** the source, **when** breakpoints are referenced, **then** they come from breakpoint tokens (SCSS) and one shared TS constant — no scattered magic numbers.

### User Story 7 - Accessibility (Priority: P3)

As a keyboard/assistive-technology user, I want the app to be operable and perceivable, so that no capability is lost for users with disabilities.

**Why this priority**: Practical accessibility is a quality bar for a production fintech product; effort is bounded to the current surface area.

**Independent Test**: Can be fully tested with a keyboard-only walkthrough (login → wallets → deposit → dialogs → command palette) and an automated contrast/focus audit.

**Acceptance Scenarios**:

1. **Given** the app, **when** traversed by keyboard only, **then** every interactive element is reachable and shows a gold focus ring; no focus is trapped (except inside dialogs/panels where it is intentionally trapped).
2. **Given** a skip-to-content link, **when** present, **then** it is the first focusable element and jumps past the sidebar/header.
3. **Given** all icon-only buttons, **when** inspected, **then** they expose an `aria-label`; no clickable `div` acts as a button.
4. **Given** form controls, **when** inspected, **then** labels and error messages are programmatically associated (`aria-describedby`/`aria-invalid`).
5. **Given** contrast, **when** spot-checked, **then** body text meets AA on the zinc surfaces and gold is used for accents/large text where it passes; status colors read against their backgrounds.
6. **Given** `prefers-reduced-motion`, **when** enabled, **then** animations/skeleton shimmer/count-up are reduced or disabled (global rule honored).

### User Story 8 - Evidence Refresh (Priority: P3)

As an engineering team, I want all UI-facing evidence artifacts to reflect the new design, so that documentation and acceptance evidence are not stale.

**Why this priority**: Evidence is part of the Aegis quality bar (see constitution); shipping the redesign while old-UI screenshots remain tracked would be misleading.

**Independent Test**: Can be fully tested by re-running the evidence pipeline (e2e suite + manual Playwright captures) and diffing old vs. new screenshots.

**Acceptance Scenarios**:

1. **Given** the live stack and a green e2e run on the new build, **when** evidence is re-captured, **then** `evidence/01–08*.png` reflect the new design with the same filenames and scenarios (references stay valid).
2. **Given** regenerable artifacts, **when** the e2e run completes, **then** `evidence/e2e/results.json` and `evidence/html-report/` are refreshed.
3. **Given** `e2e/README.md`, **when** captions describe UI elements changed by the redesign, **then** captions are updated (structure and Spanish language preserved).
4. **Given** non-UI evidence (Kafka `09–12`, `load*/`, `observability/`), **when** the refresh runs, **then** it is left untouched.
5. **Given** the final review (gate G8), **when** inspected, **then** no tracked screenshot of the old UI remains in the repository.

---

### Edge Cases

- **Undefined/legacy tokens:** any component referencing a CSS custom property that does not exist (e.g. `--aegis-surface-success`) must be found and fixed; a grep gate asserts zero undefined-token references.
- **Mixed-currency totals:** the aggregate total across currencies must never be shown with a wrong currency symbol.
- **Idempotent deposit (409):** redesign must preserve the duplicate-reference rejection and its toast; receipt must not flash in a light palette.
- **Session expiry during flow:** redirect-to-login behavior (interceptor/guard) must remain intact; no infinite redirects or infinite loading.
- **Theme removal:** removing the light theme must not leave stale `data-theme` handling or dead `ThemeService` consumers; `prefers-color-scheme` listeners removed with the service.
- **Empty/error API states:** wallets list and auth calls handle 4xx/5xx and network errors without infinite spinners; `isLoading` resets on success, error and timeout (never in `next`/`error` handlers alone).
- **Keyboard/ARIA regressions:** focus traps, focus restoration after closing dialogs/panels/palette, and Escape handling must not regress.
- **Evidence drift:** adding new UI flows after Phase 11 must not reintroduce stale screenshots (checked at G8 and before close).
- **Light theme (deprecated):** if a user previously persisted a light-theme preference, the app must safely fall back to dark without errors.

## Requirements

### Design Foundation (cross-cutting prerequisite, T1)

The token layer is re-pointed, not redesigned; consumers are untouched because token names stay stable.

- **DF-001**: Color tokens adopt the portfolio palette — zinc scale (foreground `#fafafa` → background `#09090b`, surfaces `#111113`/`#18181b`) and gold scale centered on `#d4af37` (accent `#e6c15a`); semantic aliases (`--aegis-color-bg`, `--aegis-surface-card`, `--aegis-border-default`, `--aegis-text-*`, semantic success/warning/error/info) preserved by name.
- **DF-002**: Add `--aegis-font-display` (Geist, loaded via the existing Google Fonts `<link>` — no new dependency) applied to headings, KPI values and brand marks.
- **DF-003**: Add token categories: z-index, breakpoints (480/768/1024/1280), icon sizes, focus ring (2px gold, 2px offset). Fix duplicate gold 200/300 and casing; define the missing `--aegis-surface-success` / `--aegis-border-success`.
- **DF-004**: Theme is **dark-only**: delete `styles/themes/_light.scss`, the light color-token mixin, `theme-toggle` component and `ThemeService`; set `data-theme="dark"` statically; `index.html` `theme-color` → `#09090b`. `_dark.scss` imports the palette from `tokens/_colors.scss` (single source of truth).
- **DF-005**: `IconRegistryService.register()` runs via `APP_INITIALIZER`; custom `aegis-*` icons render.

### Functional Requirements

- **FR-001**: All color, radius, shadow, spacing and typography values in component SCSS MUST come from `--aegis-*` tokens; zero hex literals in component styles outside `styles/tokens/` (enforced by a grep gate in CI-quality checks).
- **FR-002**: Every CSS custom property referenced in styles MUST be defined — zero `var(--undefined, fallback)` references (grep gate).
- **FR-003**: Interactive components MUST expose hover / focus-visible / disabled / loading states where applicable, using token values.
- **FR-004**: Financial figures MUST use the mono font with `font-variant-numeric: tabular-nums`; currency formatting MUST be centralized in one pipe/util reused across cards, KPIs and receipts.
- **FR-005**: No horizontal overflow at 390px; long values truncate with ellipsis + `title`.
- **FR-006**: The shell MUST hide the sidebar off-canvas on mobile and open it as an overlay drawer (backdrop, Escape, route-change close, scroll-lock).
- **FR-007**: The header MUST NOT display fabricated notification counts or Material `warn`-red badges; breadcrumb MUST come from route `data.title`.
- **FR-008**: Login navigation MUST NOT use `setTimeout` delays; success feedback via toast; loading buttons reset via `finalize` on success, error and timeout.
- **FR-009**: Registration MUST link back to login and provide a "Continue to sign in" action after success.
- **FR-010**: Wallet balances MUST use semantic sign colors; premium badge MUST use gold tokens; deposit receipt MUST use defined semantic success tokens.
- **FR-011**: Dead wallet actions (Transfer/Withdraw) MUST remain visible but disabled with a "Coming soon" tooltip (decision A8).
- **FR-012**: Dialogs/panels MUST trap focus and restore focus on close; overlay uses `--aegis-surface-overlay`.
- **FR-013**: A skip-to-content link, programmatic label/error association, `aria-label` on icon-only controls, and semantic buttons (no clickable divs) MUST be in place.
- **FR-014**: `prefers-reduced-motion` MUST be honored (global rule; verify shimmer/pulse/count-up).
- **FR-015**: Placeholder pages MUST use the tokenized placeholder/empty-state pattern; no inline styles with literal px/hex.
- **FR-016**: UI-facing evidence artifacts (`evidence/01–08*.png`, `results.json`, `html-report/`, `e2e/README.md` captions) MUST be regenerated with the new design before close; non-UI evidence untouched.
- **FR-017**: Existing functionality MUST be preserved — auth flows, wallet CRUD/deposit, routing, guards, interceptors, proxy config and API contracts unchanged.

### Key Entities (UI artifacts — no backend data entities)

- **Design Token**: atomic visual value (color/typography/spacing/radius/shadow/transition/z-index/breakpoint/icon-size) emitted as a CSS custom property under `--aegis-*`. Source of truth: `src/styles/tokens/`.
- **Theme**: dark-only application of Material M3 `mat.define-theme` + token overrides, activated via static `data-theme="dark"`. Source: `src/styles/themes/`.
- **Shared Component**: reusable UI unit (loading-button, status-chip, stat-card, empty-state, loading-skeleton, toast, confirmation-dialog, password-input, form-field-error, command-palette) with documented state coverage. Source: `src/app/shared/`.
- **Shell**: `app-shell` composition of sidebar + header + router-outlet + global overlays. Source: `src/app/shared/layout/`.
- **UI Evidence**: tracked screenshots/reports proving UI behavior (`evidence/01–08*.png`, `evidence/e2e/`, `e2e/README.md`).

## Success Criteria

### Measurable Outcomes

- **SC-001**: `npm run lint`, `npm run build` (budgets respected) and `npm test` (all specs, including updated theme/header/auth/wallet specs) are green on every phase gate.
- **SC-002**: The full E2E suite (`e2e/`, Playwright) is green against the running sandbox — zero regressions in auth/wallet flows.
- **SC-003**: Zero console errors across all routes; zero undefined-token references and zero hex literals in component SCSS (grep gates pass).
- **SC-004**: No horizontal overflow at 390px; main flows usable at 1440/1280/1024/768/480/390.
- **SC-005**: Keyboard-only walkthrough (login → wallets → deposit → dialogs → palette) succeeds; contrast spot-checks pass.
- **SC-006**: UI evidence (`evidence/01–08*.png` + e2e report) shows the new design; zero tracked old-UI screenshots remain (gate G8).
- **SC-007**: The app visually matches the portfolio design language (side-by-side review) while keeping product density.

## Assumptions

- **A1 (D1)**: Full portfolio alignment is confirmed — token values re-point to zinc + gold `#d4af37` + Geist; token names stay stable.
- **A2 (D2)**: Dark-only theme confirmed; light theme, toggle and `ThemeService` are removed.
- **A3 (D3)**: Building Dashboard/Transactions/Settings/Payments features is out of scope — placeholders remain placeholders; only the placeholder pattern is modernized.
- **A4**: Auth mechanics are out of scope and believed intentional: BFF cookie sessions (spec `010-bff`), no-op `http-auth.interceptor`, dev `enableMockLogin` flag. Flagged for team review; behavior unchanged in this initiative.
- **A5**: Verification requires the live stack — BFF on `:8082` via `proxy.conf.json`, frontend served (dev or nginx `:4200`), registered test user (per `e2e/README.md`), and `infra/docker-compose.yml` for evidence re-capture.
- **A6**: No new npm dependencies; Geist loads via the existing Google Fonts `<link>` (available on Google Fonts).
- **A7 (resolved)**: Mixed-currency total presents a **per-currency aggregate** (e.g. `€ 150.00 · $ 200.00`) via one shared currency-formatting pipe.
- **A8 (resolved)**: Transfer/Withdraw buttons remain visible but **disabled with a "Coming soon" tooltip**.
- **A9 (resolved)**: Notification bell and badge are **removed from the header** until a real feed exists.
- **A10 (resolved)**: `ThemeService` and `theme-toggle` are **removed** together with the light theme; verify no other consumers before removal.
- **A11**: Spec/plan/tasks live under `specs/013-frontend-ui-modernization/`; branch `feature/013-frontend-ui-modernization`; epic + sub-issues created via `issue-manager` at the issues step; one PR per task group.

## Clarifications (resolved in `/speckit.clarify` — gate G2)

| ID | Question | Decision |
|----|----------|----------|
| A7 | Mixed-currency total balance presentation | **Per-currency aggregate** (`€ 150.00 · $ 200.00`) via one shared currency pipe. No single misleading `$`. |
| A8 | Dead Transfer/Withdraw buttons | **Keep visible, disabled** with "Coming soon" tooltip (stable layout, honest affordance). |
| A9 | Fake notification count in header | **Remove bell + badge** until a real notification feed exists. |
| A10 | `ThemeService` + `theme-toggle` under dark-only | **Remove both** (plus `prefers-color-scheme` listener); static `data-theme="dark"`. Verify no other consumers first. |

## Out of Scope (verbatim from strategy plan §4)
- Building Dashboard/Transactions/Settings/Payments features (placeholders stay placeholders).
- Backend, BFF, API contract, proxy, or environment changes.
- Auth/session mechanics (guard, interceptors, mock-login flag).
- New npm dependencies or UI libraries (Angular Material stays; Geist loads via the existing Google Fonts `<link>`).
- Light theme (deprecated per D2).
