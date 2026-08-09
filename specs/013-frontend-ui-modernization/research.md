# Research: UC-013 Frontend UI/UX Modernization

**Feature**: 013-frontend-ui-modernization | **Branch**: `feature/013-frontend-ui-modernization`
**Purpose**: Phase 0 research — portfolio visual extraction, current-state audit, and verification of design decisions feeding `plan.md`.

## 1. Sources

| Source | Role |
|--------|------|
| `Aegis-Portfolio/src/styles/global.css` + `src/components/*.astro` | Reference visual language (Astro + Tailwind, dark-only) |
| `frontend/aegis-frontend/src/styles/tokens/*.scss` | Current design-token system |
| `frontend/aegis-frontend/src/styles/themes/*.scss`, `theme.scss` | Current Material M3 theming |
| `frontend/aegis-frontend/src/app/**` | Component audit (shell, header, sidebar, wallet, auth, registration, shared) |
| `frontend/aegis-frontend/src/index.html`, `angular.json`, `package.json`, `proxy.conf.json` | Assets, budgets, stack, proxy |
| `evidence/`, `e2e/README.md` | Evidence inventory (gate G8) |

## 2. Portfolio visual language (extracted)

| Category | Value | Notes |
|----------|-------|-------|
| Background | `#09090b` (zinc-950) | body bg |
| Surface | `#111113` | panels, code blocks |
| Card | `#18181b` (zinc-900) | cards |
| Primary gold | `#d4af37` | brand accent |
| Accent gold | `#e6c15a` | hovers, links |
| Foreground | `#fafafa` (zinc-50) | text |
| Muted | `#a1a1aa` (zinc-400) | secondary text |
| Borders | `rgb(255 255 255 / 0.06–0.10)` | hairlines; hover → gold 40% |
| Radius | `0.375rem`–`1rem` | inputs → panels/diagrams |
| Focus ring | `2px solid #d4af37`, offset 2px | `:focus-visible` |
| Selection | gold 30% alpha | `::selection` |
| Fonts | Inter (body), **Geist** (display), JetBrains Mono (code/data) | Geist = Vercel display font |
| Motion | 150–200ms; `cubic-bezier(0.22,1,0.36,1)` for entrances | restrained |
| Atmosphere | subtle grid (`rgb(255 255 255/0.03)` lines 56px) + soft gold radial glow (`rgba(212,175,55,0.14)`) | hero/section backdrop, not content |

## 3. Current frontend state (audit summary)

- **Stack:** Angular 22 standalone, Material M3 (`mat.define-theme`, density −2), signals, Karma/Jasmine, ESLint. Budgets: initial 600kB warn / 1.2MB error; `anyComponentStyle` 6kB warn / 10kB error.
- **Token system exists** and is the right mechanism: `tokens/_colors|typography|spacing|radius|shadows|transitions.scss` emit `--aegis-*` CSS vars. **Strategy: re-point values, keep names** → consumers untouched.
- **Real pages:** auth (login), registration, wallet. **9 placeholder routes** via `PagePlaceholderComponent`.
- **Material theming:** M3 `mat.define-theme` + `data-theme` attribute; dark = default & complete, light = incomplete (deprecated by D2).

### Defects found (file-referenced)

| # | Defect | Location |
|---|--------|----------|
| 1 | Undefined tokens `--aegis-surface-success` / `--aegis-border-success` → deposit receipt light-mint in dark | `wallet.component.scss:463-464` |
| 2 | Duplicate gold 200/300; no z-index/breakpoint/icon-size/focus-ring tokens; breakpoints 480/768/1024 & z-index 999–1300 scattered | `tokens/_colors.scss:28-29`, components |
| 3 | Duplicate palette source in dark theme | `themes/_dark.scss:28-46` |
| 4 | Mobile sidebar 64px rail stays in-flow on phones | `app-shell.component.ts:73`, `sidebar.component.scss` |
| 5 | Fake `notificationCount = signal(3)`; `matBadgeColor="warn"`; cosmetic breadcrumb; dead `.env-staging` | `header.component.ts:38,58-62` |
| 6 | Hardcoded px/hex hotspots | header (43px), wallet (38), sidebar (28); premium badge `#fbbf24/#d97706/#1a1a2e`; sign colors `#16a34a/#dc2626` |
| 7 | `:focus-visible` missing on outline/ghost button variants; `transition: all`; hardcoded `#FFFFFF` | `loading-button` |
| 8 | Overlay `rgba(0,0,0,0.6)` hardcoded; no CDK focus trap | `confirmation-dialog` |
| 9 | `IconRegistryService.register()` never invoked → `aegis-*` icons dead | `shared/icons`, `app.config.ts` |
| 10 | `setTimeout(800)` login navigation | `auth.component.ts:84` |
| 11 | Registration success dead end; no login link | `registration.component.html` |
| 12 | Unconditional `$` on mixed-currency `totalBalance` | `wallet.component.ts` |
| 13 | Placeholder pages inline styles with literal px/hex | `page-placeholder.component.ts:18-44` |
| 14 | `e2e/README.md` documents `evidence/01–08*.png` showing old UI | `evidence/` |

## 4. Token mapping: portfolio → app

Token *names* stay stable (`--aegis-*`). Only values change. Semantic aliases preserved so components need no renames.

| App token (existing) | Current value | New value (portfolio) | Note |
|---|---|---|---|
| `--aegis-color-navy-*` scale | navy `#0a0e1a` family | **zinc scale** (`#09090b`→`#fafafa`) | replace in place; keep var names for now, deprecate |
| `--aegis-color-bg` | navy-900 | `#09090b` | |
| `--aegis-surface-card` | navy-800 | `#18181b` | zinc-900 |
| `--aegis-surface-elevated` | navy-800 | `#111113` | zinc-900-ish (portfolio surface) |
| `--aegis-color-border(-default/-subtle)` | navy-500/600 | `rgb(255 255 255 / 0.10)` / `/0.06` | hairline |
| `--aegis-border-hover` | navy-400 | gold 40% | portfolio hover |
| `--aegis-gold-*` scale | `#d4a843` base | base `#d4af37`, accent `#e6c15a` | gold-400=`#e6c15a`, gold-500=`#d4af37` |
| `--aegis-color-primary` | gold-500 | `#d4af37` | |
| `--aegis-color-primary-light` | gold-400 | `#e6c15a` | |
| `--aegis-color-text` | `#e8ecf4` | `#fafafa` | |
| `--aegis-color-text-muted` | navy-300 | `#a1a1aa` | zinc-400 |
| `--aegis-color-text-secondary` | navy-50 | `#d4d4d8` | zinc-300 |
| `--aegis-font-brand` | Inter | Inter (body) | add `--aegis-font-display: Geist` |
| focus ring | 3px glow `rgba(212,168,67,0.2)` | **2px solid `#d4af37`, offset 2px** | token `--aegis-focus-ring` |
| selection | gold-subtle bg | gold 30% alpha | |
| shadows | existing scale (0.3–0.7 alpha) | keep scale; tune for zinc | minimal change |
| transitions | 75–500ms scale | keep; prefer 150–200ms | |

### New token categories (add)

- `--aegis-z-*`: dropdown 1000 / overlay 1100 / drawer 1200 / modal 1300 / toast 1400 / palette 1500
- `--aegis-breakpoint-*`: 480 / 768 / 1024 / 1280 (SCSS vars + one shared TS constant)
- `--aegis-icon-size-*`: 16 / 18 / 20 / 24 / 32
- `--aegis-focus-ring` (width/color/offset)
- Missing semantic: `--aegis-surface-success`, `--aegis-border-success` (defined; fixes receipt)

## 5. Verified decisions

| Decision | Basis | Risk / mitigation |
|----------|-------|-------------------|
| **D1** Full portfolio alignment (zinc + gold `#d4af37` + Geist) | Portfolio is the mandated visual reference; token names stable → low-touch | Visual shift is deliberate; gate every phase with screenshots vs baseline |
| **D2** Dark-only; remove light theme, `ThemeService`, `theme-toggle` | Portfolio is dark-only; light theme is incomplete (no component overrides, gold-600 on white fails 4.5:1 contrast) | Verify no other consumers of `ThemeService`/`data-theme` before removal; specs updated |
| **A7** Per-currency aggregate total (`€ 150.00 · $ 200.00`) | No misleading single symbol across currencies | New shared `currency` pipe; used by cards/KPIs/receipt |
| **A8** Transfer/Withdraw disabled + "Coming soon" tooltip | Honest affordance, stable layout, no feature change | Tooltip needs `matTooltipDisabled` handling on disabled buttons |
| **A9** Remove notification bell + badge | No fabricated data | Delete component usage; leave service/data untouched (none) |
| **A10** Remove `ThemeService`/toggle + `prefers-color-scheme` listener | Static dark; less surface area | Grep for `ThemeService` consumers first |

## 6. Risks & mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Component style budgets (6kB/10kB) exceeded after token restyle | Medium | Keep component SCSS lean; move values to tokens/global; verify with `npm run build` per gate |
| Zeppelin of legacy token aliases after re-pointing | Medium | Single naming scheme; codemod consumers in T1; grep gates |
| E2E/auth regressions from shell changes (mobile drawer, header) | Medium | Playwright suite per gate; manual flow checks |
| Light-theme removal breaks persisted preferences | Low | Static `data-theme="dark"`; no listener; safe fallback |
| Evidence staleness creep | Medium | Gate G8 as blocking close criterion; FR-016 |
| Geist not rendering (font load) | Low | Via existing Google Fonts link; `font-display: swap`; fallback Inter |

## 7. Conclusion

The modernization is a **token re-point + targeted component hardening + shell rebuild**, not a rewrite. The existing token architecture and shared components are reused. No new dependencies, no backend changes, no new features. Execution order T0–T11 with per-group quality gates; closes only after evidence gate G8.
