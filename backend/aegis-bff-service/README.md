# BFF Service

**Purpose**: Backend-for-Frontend — single entry point for the Angular SPA. Proxies auth and wallet requests, manages session-based JWT storage, and hides internal service topology from the frontend.

## Functionality

- Proxies authentication endpoints to Identity Service
- Proxies wallet CRUD, deposit, balance, and status endpoints to Wallet Service
- Manages JWT storage in Redis-backed HttpSession (HttpOnly cookie)
- Attaches JWT to proxied requests via `SessionJwtAuthenticationFilter`
- Extracts user ID from session for `X-User-Id` headers

## Architecture

```
Angular SPA → BFF WalletController / BffAuthController
  → SessionJwtAuthenticationFilter → SessionJwtStore → Redis
  → BffService (WebClient) → Identity Service / Wallet Service
```

## Tech Stack

- Java 21, Spring Boot 3.3, Spring Security
- Spring Session Data Redis
- WebClient (reactive HTTP)

## Configuration

| Property | Value |
|----------|-------|
| Port | 8082 |
| Session store | Redis |
| Identity Service URL | `http://localhost:8081` |
| Wallet Service URL | `http://localhost:8083` |

## API Endpoints

| Method | Path | Proxies To |
|--------|------|------------|
| POST | `/api/bff/auth/login` | Identity: `POST /api/v1/auth/login` |
| POST | `/api/bff/auth/logout` | Session invalidation |
| GET | `/api/bff/auth/me` | Session user info |
| POST | `/api/bff/auth/refresh` | Identity: `POST /api/v1/auth/refresh` |
| GET | `/api/bff/wallets` | Wallet: `GET /api/v1/wallets` |
| POST | `/api/bff/wallets` | Wallet: `POST /api/v1/wallets` |
| POST | `/api/bff/wallets/{id}/deposits` | Wallet: `POST /api/v1/wallets/{id}/deposits` |
| PATCH | `/api/bff/wallets/{id}/balance` | Wallet: `PATCH /api/v1/wallets/{id}/balance` |
| PATCH | `/api/bff/wallets/{id}/status` | Wallet: `PATCH /api/v1/wallets/{id}/status` |

## Key Design Decisions

| Decision | Choice |
|----------|--------|
| Session store | Redis (distributed) |
| HTTP client | WebClient (reactive) |
| CSRF | Enabled |
| Cookie | HttpOnly, SameSite=Strict |
