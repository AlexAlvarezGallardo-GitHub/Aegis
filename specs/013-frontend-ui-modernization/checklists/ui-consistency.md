# Checklist: UI Consistency & Design System Quality — UC-013 (frontend-only)

**Purpose**: Aegis-specific checklist for the frontend modernization, derived from `spec.md` (DF-001..005, FR-001..017), `research.md` §4, and the strategy plan's final checklist. Validates **requirements quality**: the spec must be consistent, measurable, and gap-free for the design system work.

## Category: Design Tokens & Anti-Drift

- [ ] CHK101 — Is the requirement explicit that all color/radius/shadow/spacing/typography values in component SCSS MUST come from `--aegis-*` tokens (zero hex literals outside `styles/tokens/`, grep-enforced)? [Measurability, Spec §FR-001]
- [ ] CHK102 — Is the requirement explicit that zero CSS custom properties may be referenced without being defined (no `var(--undefined)` fallbacks)? [Measurability, Spec §FR-002]
- [ ] CHK103 — Do token additions cover the missing categories: z-index, breakpoints (SCSS + one shared TS constant), icon sizes, focus ring, `--aegis-surface-success`/`--aegis-border-success`? [Completeness, Spec §DF-003]
- [ ] CHK104 — Is a single naming scheme specified for semantic tokens (legacy aliases consolidated, no duplicates like gold 200/300)? [Consistency, Spec §DF-001, research §3]
- [ ] CHK105 — Is dark-only specified as the single theme, with light theme/`ThemeService`/`theme-toggle`/`prefers-color-scheme` listener removed and no stale consumers? [Clarity, Spec §DF-004, A2/A10]

## Category: Portfolio Alignment

- [ ] CHK106 — Is the palette alignment specified to the portfolio values (zinc `#09090b`/`#111113`/`#18181b`, gold `#d4af37`, accent `#e6c15a`) per `research.md` §4, while keeping token names stable? [Consistency, Spec §DF-001]
- [ ] CHK107 — Is Geist specified as the display font (`--aegis-font-display`) applied to headings/KPI/brand marks, loaded via the existing Google Fonts link (no new dependency)? [Clarity, Spec §DF-002, A6]
- [ ] CHK108 — Is the portfolio's restrained atmosphere specified (hairline borders, gold focus ring 2px/offset 2px, gold selection, 150–200ms transitions)? [Consistency, Spec §DF, research §2]
- [ ] CHK109 — Is "portfolio = brand, application = product density" respected (no marketing layouts copied into the app; fintech density kept)? [Ambiguity, Spec §Solution]

## Category: Component State Coverage

- [ ] CHK110 — Does the spec require hover/focus-visible/disabled/loading/error/success states where applicable for every interactive component (all button variants included)? [Completeness, Spec §FR-003]
- [ ] CHK111 — Does the spec require dialog/panel focus trapping + focus restore on close and the overlay token (`--aegis-surface-overlay`)? [Completeness, Spec §FR-012]
- [ ] CHK112 — Does the spec require `empty-state` to hide its action when no handler exists (no dead clicks)? [Clarity, Spec §US-2 AC3]
- [ ] CHK113 — Does the spec require the custom `aegis-*` icon registry to be initialized (no console 404s)? [Completeness, Spec §US-2 AC5, DF-005]

## Category: Typography & Financial Data

- [ ] CHK114 — Is `tabular-nums` + mono required for all financial figures? [Measurability, Spec §FR-004]
- [ ] CHK115 — Is one shared currency-formatting pipe required, reused across cards/KPIs/receipt, with per-currency aggregate totals (no misleading single `$`)? [Consistency, Spec §US-4 AC3, A7]
- [ ] CHK116 — Are wallet sign colors specified via semantic tokens and the premium badge via gold tokens (no hardcoded hex)? [Consistency, Spec §US-4 AC2, FR-010]

## Category: Layout & Navigation

- [ ] CHK117 — Is the shell specified with tokenized dimensions and zinc/hairline surfaces? [Consistency, Spec §US-1 AC1]
- [ ] CHK118 — Is the mobile off-canvas sidebar with overlay drawer (backdrop, Escape, route-close, scroll-lock) specified — no in-flow rail? [Clarity, Spec §US-1 AC2]
- [ ] CHK119 — Is the header specified without fabricated notification counts/warn-red badges and with breadcrumb from route `data.title`? [Consistency, Spec §US-1 AC4, A9]
- [ ] CHK120 — Is the placeholder pattern specified for all 9 routes (tokenized, route title, no inline px/hex)? [Completeness, Spec §US-5]

## Category: Responsive

- [ ] CHK121 — Is no-horizontal-overflow at 390px and ellipsis+`title` truncation for long values specified? [Measurability, Spec §FR-005]
- [ ] CHK122 — Are intentional mobile layouts specified for grids (4→2→1), panels (full-width ≤480px), forms and palette? [Coverage, Spec §US-6 AC2]

## Category: Accessibility

- [ ] CHK123 — Are skip-link, keyboard reachability, gold focus rings, ARIA labels on icon-only controls, and semantic buttons (no clickable divs) specified? [Completeness, Spec §FR-013, US-7]
- [ ] CHK124 — Is `prefers-reduced-motion` compliance specified for shimmer/pulse/count-up? [Completeness, Spec §US-7 AC6]
- [ ] CHK125 — Is AA contrast spot-checking specified for text on zinc and for gold accents? [Measurability, Spec §US-7 AC5]

## Category: UX States & Functionality Preservation

- [ ] CHK126 — Are loading (skeleton = content dims, no CLS), empty, and error-with-retry states specified for the wallet page? [Coverage, Spec §US-4 AC6]
- [ ] CHK127 — Is `isLoading` reset via `finalize` (success/error/timeout) specified — never relying on `next`/`error` alone? [Completeness, AGENTS.md, Spec §US-3 AC2]
- [ ] CHK128 — Is functional preservation explicit: auth flows, wallet CRUD/deposit, routing, guards, interceptors, proxy and API contracts unchanged (FR-017)? [Consistency, Spec §Out of Scope]
- [ ] CHK129 — Is "no new dependencies" and "no new feature pages" explicit in the spec? [Clarity, Spec §Out of Scope]

## Category: Evidence

- [ ] CHK130 — Is UI evidence regeneration (same filenames/scenarios, `e2e/README.md` captions, `results.json`, `html-report/`) specified, with Kafka/load/observability untouched? [Completeness, Spec §US-8, FR-016]

## Review note

This checklist is executed at gate **G7** (requirements quality) and re-checked at **G8/review-implement** for implementation conformance. Items reference spec sections for ≥80% traceability; no duplicates with security/testing checklists.
