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
| 1 | `01-login-filled.png` | Formulario de login (diseño zinc + logo Aegis) |
| 2 | `02-wallets.png` | Lista de wallets (info de negocio, sin UUIDs) |
| 3 | `03-wallet-detail.png` | Full-page Wallet Detail (header, balance overview, tabs) |
| 4 | `04-deposit-modal.png` | Modal de depósito (amount/source/reference) |
| 5 | `05-deposit-success-toast.png` | Toast compacto de depósito completado |
| 6 | `06-dup-reference-rejected.png` | Referencia duplicada rechazada (toast) |
| 7 | `07-create-wallet-form.png` | Crear wallet (form) |
| 8 | `08-two-wallets.png` | Varias wallets en la lista |
| 9–12 | `09-…`–`12-…` | Topics Kafka (backend) |
| — | `mobile/` · `toast/` · `uuid/` | Evidencia por feature — ver [`evidence/README.md`](../evidence/README.md) |
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
