# Playwright E2E Suite — Aegis Frontend

Evidencia automatizada de los casos de uso implementados en el frontend Angular.

## Prerrequisitos

1. Stack completo corriendo:
   ```bash
   docker compose -f infra/docker-compose.yml up -d
   ```
2. Frontend servido en `http://localhost:4200` (nginx del stack).
3. Un usuario registrado (por defecto `alex@aegis.test` / `StrongPass123!`). Se puede
   sobreescribir con las variables `AEGIS_E2E_EMAIL` / `AEGIS_E2E_PASSWORD`.

## Instalación

```bash
cd e2e
npm init -y
npm install -D @playwright/test
npx playwright install chromium
```

## Ejecución

```bash
npx playwright test --config=playwright.config.ts
```

Reporte HTML generado en `evidence/html-report`.

## Casos de uso cubiertos

| Spec | Casos de uso | Verifica |
|------|--------------|----------|
| `tests/auth.spec.ts` | UC-001/UC-002 | Login válido, validación del botón, credenciales inválidas |
| `tests/wallet.spec.ts` | UC-003/UC-004 | Crear wallet, depósito con source+reference, recibo, idempotencia (409) |

## Evidencia manual (esta sesión)

Capturas obtenidas con Playwright contra el stack en vivo, en `evidence/`:

| # | Archivo | Evidencia |
|---|---------|-----------|
| 1 | `01-login-filled.png` | Formulario de login completo (nuevo diseño zinc + logo Aegis) |
| 2 | `02-wallets-premium.png` | Lista de wallets (badge PREMIUM aparece solo si el backend marca la wallet como premium — ver `wallet.model` `premium`) |
| 3 | `03-wallet-detail-deposit-section.png` | Sección **Deposit Funds** del detalle |
| 4 | `04-deposit-form-filled.png` | Formulario de depósito (amount/source/reference) |
| 5 | `05-deposit-receipt.png` | Recibo: 150 EUR from BANK_TRANSFER (ref `UI-MODERNIZATION-001`) |
| 6 | `06-dup-reference-rejected.png` | Referencia duplicada rechazada (balance sin cambios) |
| 7 | `07-create-wallet-form.png` | Crear wallet (form EUR) |
| 8 | `08-two-wallets.png` | 2 wallets creadas, 2 monedas |
| 9 | `09-kafka-topics-list.png` | Topics con mensajes (wallet.funds.deposited=7, fraud=6) |
| 10 | `10-kafka-topic-wallet-funds.png` | Topic `wallet.funds.deposited` — 7 eventos |
| 11 | `11-kafka-wallet-funds-messages.png` | Payload de los eventos FUNDS_DEPOSITED |
| 12 | `12-kafka-fraud-topic.png` | Topic `fraud.assessment.completed` — 6 assessments |

## Verificación backend de la evidencia

```bash
# Reporting recibió el evento del depósito hecho por la UI
docker exec aegis-postgres-reporting psql -U aegis -d aegis_reporting \
  -c "SELECT wallet_id, balance, last_updated FROM balance_projections;"

# Audit registró el depósito y los assessments de fraude
docker exec aegis-postgres-audit psql -U aegis -d aegis_audit \
  -c "SELECT * FROM audit_records WHERE reference='PLAYWRIGHT-E2E-001';"
docker exec aegis-postgres-audit psql -U aegis -d aegis_audit \
  -c "SELECT COUNT(*) FROM fraud_audit_records;"
```
