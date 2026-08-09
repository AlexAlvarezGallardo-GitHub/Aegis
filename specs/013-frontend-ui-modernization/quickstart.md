# Quickstart: UC-013 Frontend UI/UX Modernization

How to run and verify the frontend during this feature. Works on Windows (pwsh) unless noted.

## Prerequisites

- Node.js (LTS) + npm — required for the Angular app and e2e
- Docker Desktop — required to run the full stack (`infra/docker-compose.yml`) for evidence and e2e
- (Optional) JDK 21 + Maven — only if running services natively instead of Docker
- A registered test user for e2e/manual flows: `alex@aegis.test` / `StrongPass123!` (override via `AEGIS_E2E_EMAIL` / `AEGIS_E2E_PASSWORD`)

## 1. Full stack (recommended — needed for manual runs, e2e, evidence)

```powershell
# from repo root
docker compose -f infra/docker-compose.yml up -d
```

Services you care about:
- BFF (auth session, wallet proxy): `http://localhost:8082`
- Identity: `http://localhost:8081`
- Frontend (nginx static, prod build): `http://localhost:4200`

### Dev mode with hot reload

```powershell
infra\build-and-run.bat dev
# or
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d
```

Uses `Dockerfile.dev` → `ng serve --poll 1000` on `:4200`, source mounted from `./src`.

## 2. Frontend only (native, uses `proxy.conf.json` → localhost:8081/8082)

```powershell
cd frontend/aegis-frontend
npm install
npm start            # ng serve --poll 1000 → http://localhost:4200
```

Requires the backend services (or full stack) to be up for anything beyond the login screen.

## 3. Verification commands (run per phase gate)

```powershell
cd frontend/aegis-frontend

npm run lint         # ESLint (angular-eslint) — must be clean
npm run build        # prod build — must pass budgets (600kB/1.2MB initial, 6kB/10kB component styles)
npm test             # Karma/Jasmine unit tests (ChromeHeadless) — must be green
```

> Karma needs Chrome. If a headless run fails locally, run `npm test` in the dev Docker container or install Chrome.

## 4. E2E (Playwright) — regressions & evidence

```powershell
# full stack must be up (step 1), frontend on :4200, test user registered
cd e2e
npm install            # first time only
npx playwright install chromium   # first time only
npx playwright test --config=playwright.config.ts
```

Reporte HTML: `evidence/html-report`. Results: `evidence/e2e/results.json`.

## 5. Evidence refresh (T11 / gate G8)

Re-capture the UI screenshots with the same filenames (`evidence/01–08*.png`) against the live stack using the same scenarios documented in `e2e/README.md` (login, wallets+premium, deposit form/filled, deposit receipt, 409 duplicate-reference, create wallet, two wallets). After capture, regenerate the HTML report and update `e2e/README.md` captions where the redesign changed what is shown.

## 6. Useful tips

- Proxy dev vs docker: `proxy.conf.json` (local) → `localhost:8081/8082`; `proxy.conf.docker.json` (in-container) → service names.
- After structural changes that affect specs, update and re-run the affected Karma specs before the phase gate.
- Backend verification of evidence (from `e2e/README.md`): query `aegis_reporting.balance_projections` and `aegis_audit.audit_records` in the Postgres containers to prove a UI deposit actually landed.
