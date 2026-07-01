# Aegis Design System

Enterprise fintech payment platform — visual identity and component system.

---

## Brand Identity

| Property | Value |
|---|---|
| **Name** | Aegis |
| **Meaning** | Protection, resilience, trust, security |
| **Logo** | Metallic gold shield on dark navy background |
| **Tagline** | *(optional)* |

### Design Principles

1. **Enterprise First** — Built for financial institutions, not consumers
2. **Information Dense** — Maximize data-in-view without clutter
3. **Minimal Visual Noise** — Purposeful every element earns its place
4. **Accessibility First** — WCAG 2.1 AA minimum, AAA preferred
5. **Functional Beauty** — Aesthetics serve usability, not the reverse
6. **Design Consistency** — One system, one language, everywhere
7. **Dark Mode Native** — Dark-first, light is the exception
8. **Dashboard Driven** — Every view is a dashboard or a modal
9. **Security Oriented** — Visual cues reinforce trust and safety
10. **Premium Experience** — Stripe/Ramp/Brex calibre polish

### Visual References

Stripe Dashboard · Linear · Ramp · Brex · GitHub · Vercel · Datadog · Grafana · AWS Console

---

## Color Palette

### Brand Colors

| Token | Hex | Usage |
|---|---|---|
| `--aegis-gold` | `#C89B3C` | Primary brand color, CTAs, active states |
| `--aegis-gold-light` | `#E2B859` | Hover, highlights, badges |
| `--aegis-gold-dark` | `#9B7426` | Pressed states, decorative borders |
| `--aegis-gold-subtle` | `#362B00` | Container backgrounds |

### Surface & Background

| Token | Hex | Usage |
|---|---|---|
| `--aegis-bg` | `#0B1220` | Page background |
| `--aegis-bg-secondary` | `#121B2E` | Cards, sidebars, elevated areas |
| `--aegis-surface` | `#1B2538` | Inputs, table rows, dialog surfaces |
| `--aegis-surface-hover` | `#243146` | Hover state for surface elements |

### Borders

| Token | Hex | Usage |
|---|---|---|
| `--aegis-border` | `#2A3448` | Default border |
| `--aegis-border-secondary` | `#334155` | Subtle dividers |

### Typography

| Token | Hex | Usage |
|---|---|---|
| `--aegis-text` | `#F8FAFC` | Primary body text |
| `--aegis-text-secondary` | `#CBD5E1` | Labels, descriptions |
| `--aegis-text-muted` | `#64748B` | Placeholders, disabled, metadata |

### Semantic Colors

| Token | Hex | Usage |
|---|---|---|
| `--aegis-success` | `#10B981` | Approved, confirmed, healthy |
| `--aegis-warning` | `#F59E0B` | Pending, degraded, attention |
| `--aegis-error` | `#EF4444` | Failed, declined, critical |
| `--aegis-info` | `#3B82F6` | Informational, neutral updates |

---

## Typography

### Font Stack

| Role | Font |
|---|---|
| **Brand / Display** | `Inter, "Helvetica Neue", sans-serif` |
| **UI / Body** | `Inter, "Helvetica Neue", sans-serif` |
| **Monospace (optional)** | `"Geist Mono", "JetBrains Mono", monospace` |

### Font Weights

| Weight | Usage |
|---|---|
| 400 (Regular) | Body text, paragraphs |
| 500 (Medium) | Labels, buttons, table headers |
| 600 (Semibold) | Subheadings, card titles |
| 700 (Bold) | Page titles, metric values |

### Type Scale

| Level | Size | Weight | Line Height | Usage |
|---|---|---|---|---|
| Display | 32px / 2rem | 700 | 1.2 | Page titles, metric hero values |
| Heading 1 | 24px / 1.5rem | 600 | 1.3 | Section headers |
| Heading 2 | 20px / 1.25rem | 600 | 1.4 | Card titles, panel headers |
| Heading 3 | 16px / 1rem | 600 | 1.5 | Subtle headings |
| Body | 14px / 0.875rem | 400 | 1.5 | Default text |
| Body Small | 13px / 0.8125rem | 400 | 1.5 | Metadata, timestamps |
| Caption | 12px / 0.75rem | 500 | 1.4 | Labels, badges, tabular data |
| Mono | 13px / 0.8125rem | 400 | 1.5 | Code, IDs, amounts |

---

## Spacing System

Based on a 4px grid:

| Token | Value |
|---|---|
| `--aegis-space-1` | 4px |
| `--aegis-space-2` | 8px |
| `--aegis-space-3` | 12px |
| `--aegis-space-4` | 16px |
| `--aegis-space-5` | 20px |
| `--aegis-space-6` | 24px |
| `--aegis-space-8` | 32px |
| `--aegis-space-10` | 40px |
| `--aegis-space-12` | 48px |
| `--aegis-space-16` | 64px |

---

## Elevation System

| Token | Value | Usage |
|---|---|---|
| `--aegis-shadow-sm` | `0 1px 2px rgba(0,0,0,0.3)` | Subtle divide |
| `--aegis-shadow-md` | `0 4px 12px rgba(0,0,0,0.25)` | Cards, dropdowns |
| `--aegis-shadow-lg` | `0 8px 24px rgba(0,0,0,0.3)` | Dialogs, modals |
| `--aegis-shadow-xl` | `0 12px 40px rgba(0,0,0,0.4)` | Sidebars, overlays |

---

## Radius System

| Token | Value | Usage |
|---|---|---|
| `--aegis-radius-sm` | 4px | Inputs, small badges |
| `--aegis-radius-md` | 6px | Buttons, cards |
| `--aegis-radius-lg` | 8px | Dialogs, panels |
| `--aegis-radius-xl` | 12px | Large cards, modals |

---

## Component Library

### Buttons
- **Primary** — Gold fill (#C89B3C), dark text
- **Secondary** — Outline with border (#2A3448)
- **Ghost** — No background, text only
- **Danger** — Red (#EF4444)
- **Icon** — Square, icon-only (tooltip required)
- Sizes: sm (32px), md (40px), lg (48px)

### Inputs & Selects
- Dark surface (#1B2538), gold focus ring
- Outlined style (Material `appearance="outline"`)
- Inline error messages with semantic color

### Tables & Data Grids
- Compact rows (40px height)
- Sticky headers with surface background
- Alternating row colors (even rows use surface-hover)
- Sortable columns, column resize, row selection
- Pagination at bottom, 25/50/100 rows per page

### KPI Widgets
- Metric value in Display type scale (32px, bold)
- Label below in Body Small (13px, muted)
- Sparkline or trend indicator (green up / red down)
- Optional: comparison delta (vs previous period)

### Cards
- Surface (#1B2538) background
- Border (#2A3448) radius 8px
- Padding: 20px (space-5)
- Optional hover elevation bump

### Status Indicators
- Dot + text pattern (12px dot, 4px gap)
- Success: #10B981
- Warning: #F59E0B (pulsing animation)
- Error: #EF4444
- Info: #3B82F6
- Neutral: #64748B (gray dot)

### Alerts & Toast
- Banners: full-width, semantic left border (4px)
- Toasts: bottom-right, 5s auto-dismiss
- Icons for each severity level

### Dialogs
- Dark overlay (rgba(0,0,0,0.6))
- Max width: 480px (standard), 640px (large)
- Title + description + actions (right-aligned)

### Navigation
- **Sidebar** — 240px wide, dark bg (#0B1220), gold active indicator
- **Top bar** — 56px, surface bg (#121B2E), breadcrumbs + actions
- **Breadcrumbs** — Text-secondary, gold current page

### Loading States
- Skeleton shimmer for cards and tables
- Mat-spinner for action buttons (18px)
- Full-page loader for initial route load

### Empty States
- Centered illustration or icon (64px)
- Title: "No [items] yet"
- Description and CTA button

---

## Angular Material Theme

### Configuration (`theme.scss`)

- **Theme type**: dark
- **Primary seed**: #C89B3C (gold)
- **Tertiary seed**: #3B82F6 (blue)
- **Font**: Inter
- **Density**: -1 (compact dashboard density)

### Overrides

Custom CSS variables override Material system tokens for full brand control:

- `--mat-sys-primary` → `#C89B3C`
- `--mat-sys-background` → `#0B1220`
- `--mat-sys-surface` → `#121B2E`
- etc. (see `theme.scss` for full mapping)

### Component-specific Overrides

- All `mat-form-field` → `appearance="outline"`
- Table row height → 40px via density
- Dialog max-width → 480px / 640px

---

## Layout System

### Grid

12-column responsive grid:

| Breakpoint | Min Width | Columns | Gutter |
|---|---|---|---|
| Mobile | 0 | 4 | 16px |
| Tablet | 768px | 8 | 24px |
| Desktop | 1024px | 12 | 24px |
| Wide | 1440px | 12 | 32px |

### Dashboard Layout

```
┌─────────────────────────────────────────────────┐
│ Top Bar (56px)                                   │
├──────────┬──────────────────────────────────────┤
│          │                                       │
│ Sidebar  │  Main Content Area                    │
│ (240px)  │  ┌─────┬─────┬─────┬─────┐           │
│          │  │ KPI │ KPI │ KPI │ KPI │           │
│          │  ├─────┴─────┼─────┴─────┤           │
│          │  │ Chart     │ Chart     │           │
│          │  ├───────────┴───────────┤           │
│          │  │ Table / Data Grid     │           │
│          │  └───────────────────────┘           │
└──────────┴──────────────────────────────────────┘
```

---

## Navigation Architecture

```
Dashboard
├── Overview
├── Payments
│   ├── Transactions
│   ├── Batches
│   └── Reconciliation
├── Wallets
│   ├── Accounts
│   └── Transfers
├── Merchants
│   ├── Directory
│   └── Settlements
├── Fraud Detection
│   ├── Rules
│   ├── Cases
│   └── Analytics
├── Audit
│   ├── Event Log
│   └── Compliance
├── Reporting
│   ├── Standard Reports
│   └── Custom Reports
├── Monitoring
│   ├── System Health
│   └── Alerts
└── Administration
    ├── Users
    ├── Roles
    └── Settings
```

---

## Iconography

- **Library**: Material Icons (Google Fonts CDN)
- **Style**: Outlined (filled only for semantic status)
- **Size**: 20px (standard UI), 24px (nav items), 16px (inline)
- **Color**: Inherits text color, gold for active states

---

## Accessibility

- All color combinations meet WCAG 2.1 AA contrast (4.5:1 text, 3:1 large text)
- Gold on dark backgrounds tested for readability
- Focus indicators: 2px gold outline with 2px offset
- ARIA labels on all icon-only controls
- Keyboard navigation for all interactive elements
- Reduced motion media query respects prefers-reduced-motion
- Screen reader announcements for dynamic content changes

---

## File Structure

```
src/
├── theme.scss                 # Angular Material theme config + CSS overrides
├── styles.scss                # Design tokens, global styles, utility classes
├── index.html                 # Root HTML (title, fonts, meta)
└── public/
    └── assets/
        └── logo.svg           # Aegis gold shield logo
```

---

*Maintained by the Aegis frontend team. Last updated: 2026-07-02.*
