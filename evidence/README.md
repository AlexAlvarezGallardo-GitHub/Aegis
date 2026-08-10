# Aegis — Evidence Index

Evidencia visual y de pruebas organizada **por feature**. Cada feature corresponde a un commit/PR de la rama `feature/013-frontend-ui-modernization`.

> Regla: toda evidencia UI se captura con el estado actual del diseño. Cuando una feature cambia la UI, sus capturas se regeneran (mismos nombres de escenario) para no dejar screenshots obsoletos.

## Índice por feature

| # | Feature / PR | Commit | Capturas |
|---|--------------|--------|----------|
| 1 | **Design system & app shell** (tokens zinc+gold, dark-only, logo real, shell/header/sidebar) | `55b0138` | `01-login-filled.png` · `02-wallets.png` · `07-create-wallet-form.png` · `08-two-wallets.png` |
| 2 | **Wallet Detail full-page** (página dedicada, tabs, modales deposit/withdraw) | `bfb86a1` | `03-wallet-detail.png` · `04-deposit-modal.png` |
| 3 | ~~Responsive mobile~~ → **REVERTED** (`refactor/014-remove-mobile`) | `83dd3ba` | Capa móvil eliminada (app desktop-only). Evidencia `mobile/` borrada; toasts/header/sidebar volvieron al layout desktop. |
| 4 | **Toast notification system** (bottom-right, compacto, jerarquía, stacking) | `5f31de0` | `toast/01-toast-creation.png` · `toast/02-toast-deposit.png` · `toast/03-toast-error.png` · `toast/04-toast-stack.png` |
| 5 | **UUID cleanup** (UUIDs fuera de la UI primaria; solo en Technical details) | `45c36ac` | `uuid/technical-details-dialog.png` (+ `02-wallets.png`/`03-wallet-detail.png` regeneradas sin UUID) |
| — | **Flujos funcionales end-to-end** (login, depósito, idempotencia 409) | — | `01-login-filled.png` · `05-deposit-success-toast.png` · `06-dup-reference-rejected.png` |
| — | **Eventos Kafka / backend** (no frontend) | — | `09-…`–`12-…` (Kafka UI) |
| — | **Observability** (Grafana/Tempo) | — | `observability/` |
| — | **Progreso del proceso** (histórico por fase) | — | `unit/phase*.png` (las capturas móviles del proceso fueron eliminadas por obsoletas; la evidencia móvil actual está en `mobile/`) |

## Mapa de capturas por feature

### 1. Design system & app shell (`55b0138`)
| Archivo | Qué evidencia |
|---------|---------------|
| `01-login-filled.png` | Login con branding (logo real Aegis, zinc, card hairline) |
| `02-wallets.png` | Wallets list: KPIs, cards con **info de negocio** (sin UUID), active nav gold |
| `07-create-wallet-form.png` | Slide-over panel de creación |
| `08-two-wallets.png` | Grid con varias wallets |

### 2. Wallet Detail full-page (`bfb86a1`)
| Archivo | Qué evidencia |
|---------|---------------|
| `03-wallet-detail.png` | Página dedicada: back nav, header (currency wallet + status), balance overview, tabs |
| `04-deposit-modal.png` | Modal de depósito (amount/source/reference, botón `Deposit $150.00`) |

### 3. Responsive mobile (`83dd3ba`) — **REVERTED**
Eliminada en `refactor/014-remove-mobile` (la app vuelve a ser desktop-only): se revirtieron el drawer móvil, el header móvil, el toast móvil y la evidencia `mobile/`. No aplicar de nuevo sin decisión del equipo.

### 4. Toast notification system (`5f31de0`)
| Archivo | Qué evidencia |
|---------|---------------|
| `toast/01-toast-creation.png` | `JPY wallet created successfully` (sin ID técnico) |
| `toast/02-toast-deposit.png` | `Deposit completed` + `+$150.00 · BANK_TRANSFER` + acción `View transaction` |
| `toast/03-toast-error.png` | Error `Unable to complete deposit` + `Duplicate deposit reference.` |
| `toast/04-toast-stack.png` | Stacking vertical (2+ toasts) |

### 5. UUID cleanup (`45c36ac`)
| Archivo | Qué evidencia |
|---------|---------------|
| `uuid/technical-details-dialog.png` | Dialog secundario bajo **More actions** con Wallet ID + Copy (único lugar con UUID) |

## Reportes de pruebas
- Informe por fases + acceptance: `unit/ui-modernization-unit.md`
- E2E: `e2e/results.json` + `html-report/` (regenerados con `npx playwright test --config=playwright.config.ts`)
- Baseline histórico del diseño anterior (antes de la modernización): conservado en git (`evidence/` del commit `55b0138^`).

## Convención para nuevas features
1. Cada feature nueva añade sus capturas bajo `evidence/<feature>/`.
2. Si la feature modifica pantallas ya capturadas, **re-captura** los archivos afectados con el mismo nombre de escenario.
3. Actualiza este índice y el informe `unit/ui-modernization-unit.md`.
4. En la PR, incluye las capturas relevantes en la sección *Changes*.
