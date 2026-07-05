# Feature Specification: UC-003 Backend for Frontend (BFF)

**Feature Branch**: `feature/003-bff`

**Created**: 2026-07-05

**Status**: Draft

---

## Problem

UC-002 stores JWT tokens in `localStorage`. This is vulnerable to XSS — any injected script can steal tokens.

## Solution

BFF (Backend for Frontend) pattern: a lightweight proxy between Angular and the Identity Service that:
- Accepts login requests from Angular, forwards them to Identity Service
- Stores JWT tokens in an **HttpOnly session cookie** (inaccessible to JS)
- Angular never sees raw JWT tokens
- BFF reads tokens from session, attaches them as `Authorization: Bearer` to proxied downstream requests
- Token refresh handled transparently by the BFF

---

## Affected Services

| Service | Role |
|---------|------|
| **aegis-bff-service** | New — proxy between Angular and backend services |
| **aegis-identity-service** | Unchanged — already exposes auth endpoints |
| **frontend** | AuthService updated to remove localStorage token handling |
| **Redis** | New dependency — distributed session storage for BFF |

---

## Architecture

```
Browser (Angular) ──HttpOnly cookie──→ BFF (port 8082)
                                           │
                                           ├──→ POST /api/v1/auth/login  (Identity Service)
                                           ├──→ POST /api/v1/auth/refresh (Identity Service)
                                           ├──→ GET  /api/v1/users/*      (Identity Service)
                                           │
                                           └── Session store (Redis)
```

### Flow: Login
1. Angular → `POST /api/bff/auth/login` (email + password)
2. BFF → `POST http://identity:8081/api/v1/auth/login` (forwards request)
3. Identity Service returns JWT tokens
4. BFF strips tokens from response, stores them in Redis-backed session
5. BFF sets `Set-Cookie: SESSION=...; HttpOnly; Secure; SameSite=Strict`
6. BFF returns `{ tokenType: "Bearer", expiresIn: 900, emailVerified: true }` to Angular (no accessToken/refreshToken in body)

### Flow: Authenticated Request
1. Angular sends request with session cookie
2. BFF reads session, gets JWT tokens
3. BFF attaches `Authorization: Bearer <accessToken>` header
4. BFF forwards to downstream service

### Flow: Token Refresh
1. BFF detects 401 from downstream (expired token)
2. BFF uses refresh token from session to call Identity Service refresh endpoint
3. BFF updates session with new tokens
4. BFF retries the original request

---

## API Endpoints

### POST /api/bff/auth/login
Proxies to Identity Service login. Returns success without exposing tokens.

**Response (200)**:
```json
{ "tokenType": "Bearer", "expiresIn": 900, "emailVerified": true }
```

### POST /api/bff/auth/logout
Invalidates the session. Clears the session cookie.

### GET /api/bff/auth/me
Returns current authenticated user info from the JWT stored in session.

**Response (200)**:
```json
{ "userId": "uuid", "email": "john@example.com" }
```

### POST /api/bff/auth/refresh
Transparently refreshes the access token using the refresh token in the session.

---

## Dependencies (POM)

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-session-data-redis`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-webflux` (for WebClient proxy calls)
- `aegis-common` (for shared exceptions)

---

## Sub-Tasks

- [ ] Write spec (spec.md, plan.md, tasks.md)
- [ ] Create `aegis-bff-service` Maven module + pom.xml
- [ ] Create `BffApplication.java` main class
- [ ] Create `BffAuthController.java` (login, logout, me, refresh)
- [ ] Create `BffService.java` (WebClient proxy logic)
- [ ] Create `SecurityConfig.java` (CSRF, session management)
- [ ] Create `SessionJwtStore.java` (store/retrieve JWT from session)
- [ ] Create `JwtProxyFilter.java` (attach JWT to proxied requests)
- [ ] Add Redis to `docker-compose.yml`
- [ ] Add module to root `pom.xml`
- [ ] Update frontend `AuthService` (remove localStorage)
- [ ] Update frontend `proxy.conf.json` → BFF
- [ ] Update frontend `environment.ts` → BFF
- [ ] Write tests
