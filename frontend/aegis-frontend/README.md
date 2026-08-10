# Aegis Frontend

Angular 22 SPA with Angular Material, standalone components, signals, and reactive forms.

## Development server

```bash
npm start
```

Runs `ng serve --poll 1000` at `http://localhost:4200/`. The `--poll` flag ensures file change detection on Windows (NTFS). API calls are proxied to backend services via `proxy.conf.json` (local) or `proxy.conf.docker.json` (Docker).

## Docker Development

```bash
# From repo root — starts everything with hot-reload
infra\build-and-run.bat dev
# or
docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d
```

Uses `Dockerfile.dev` — runs `ng serve --poll 1000` inside the container with source mounted from `./src`.

## Production build

```bash
npm run build
```

Multi-stage Docker build (`Dockerfile`) serves static files via nginx. Output in `dist/aegis-frontend/`.

## Proxy Configuration

| Config | Target | Use case |
|--------|--------|----------|
| `proxy.conf.json` | `localhost:8081/8082` | Local development without Docker |
| `proxy.conf.docker.json` | `aegis-bff:8082`, `aegis-identity:8081` | Inside Docker Compose network |

## Project Structure

```
src/app/
├── features/
│   ├── auth/           # Login / mock-login (standalone component)
│   ├── registration/   # User registration form
│   └── wallet/         # Wallet management (create, list, search, detail)
├── shared/
│   ├── data-display/   # StatCard, StatusChip, EmptyState, LoadingSkeleton
│   ├── guards/         # AuthGuard — auto-auth in dev, else GET /me
│   ├── interceptors/   # httpAuth (pass-through), httpError (401 → login)
│   ├── layout/
│   │   ├── app-shell/  # Shell with sidebar + header + router-outlet
│   │   ├── header/     # User menu, search, env badge
│   │   ├── sidebar/    # Left nav: Payments, Wallets, Monitoring, Settings
│   │   └── page-placeholder/ # Stub component for unimplemented routes
│   ├── models/         # LoginRequest, LoginResponse, WalletResponse
│   ├── services/       # ToastService, CommandPaletteService, ConfirmationService
│   ├── forms/          # LoadingButton, PasswordInput, FormFieldError
│   └── utils/          # markFormGroupTouched, validation helpers
```

## Running unit tests

```bash
npm test
```

## Lint

```bash
npm run lint
```
