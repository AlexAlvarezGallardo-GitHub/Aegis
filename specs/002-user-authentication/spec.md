# Feature Specification: UC-002 User Authentication

**Feature Branch**: `feature/002-user-authentication`

**Created**: 2026-07-03

**Status**: Draft

**Input**: User description: "User Authentication — End user authenticates on the Aegis platform by providing email and password. Identity Service validates credentials, issues JWT tokens, and publishes domain events for audit services."

---

## Affected Bounded Contexts

| Service | Role | Interaction |
|---------|------|-------------|
| **Identity Service** | Primary owner | Receives login request, validates credentials, issues JWT tokens, publishes events |
| **Audit Service** | Event consumer | Receives `UserAuthenticated` event, persists immutable audit record |

---

## User Scenarios & Testing

### User Story 1 - Login with Email and Password (Priority: P1)

As a registered user, I want to authenticate with my email and password so that I can obtain a JWT access token to access protected API resources.

**Why this priority**: Authentication is the gatekeeper for all subsequent use cases (wallet, payments, transfers). Without login, no authenticated operations are possible.

**Independent Test**: Can be fully tested by submitting valid credentials via `POST /api/v1/auth/login` and verifying a JWT access token and refresh token are returned.

**Affected Services**: Identity Service

**Domain Events Published**: `aegis.identity.user-authenticated`

**Events Consumed**: None (credentials validated against stored user data)

**API Endpoints**: `POST /api/v1/auth/login`

**Acceptance Scenarios**:

1. **Given** a user exists with email `john@example.com` and password `SecureP@ss1`, **When** valid credentials are submitted, **Then**:
   - The system returns HTTP `200 OK` with access token (JWT), refresh token, token type (`Bearer`), and expiry information
   - The JWT contains the user's ID, email, roles, issued-at and expiration timestamps
   - Event `aegis.identity.user-authenticated` is published with payload: `{ userId, email, timestamp, success: true }`
   - Audit Service receives the event and persists an immutable audit record

2. **Given** a user exists with email `john@example.com`, **When** an incorrect password is submitted, **Then**:
   - The system returns HTTP `401 Unauthorized` with standard error response `{ code: "INVALID_CREDENTIALS", message: "...", details: null, timestamp: "..." }`
   - Event `aegis.identity.user-authenticated` is published with payload: `{ userId, email, timestamp, success: false }`
   - The failed attempt counter is incremented for the user account
   - No token is issued

3. **Given** a user with email `nonexistent@example.com`, **When** login is attempted, **Then**:
   - The system returns HTTP `401 Unauthorized` with error code `INVALID_CREDENTIALS`
   - Response time is consistent regardless of whether the email exists (prevent email enumeration via timing)

4. **Given** a user account is in `LOCKED` status due to excessive failed attempts, **When** correct credentials are submitted, **Then**:
   - The system returns HTTP `401 Unauthorized` with error code `ACCOUNT_LOCKED`
   - No token is issued

5. **Given** a user account is in `SUSPENDED` status, **When** correct credentials are submitted, **Then**:
   - The system returns HTTP `401 Unauthorized` with error code `ACCOUNT_SUSPENDED`
   - No token is issued

---

### User Story 2 - Token Refresh (Priority: P2)

As an authenticated user, I want to refresh my access token using a refresh token so that I can maintain my session without re-entering my credentials.

**Why this priority**: Token refresh is essential for user experience but can be deferred slightly since short-lived access tokens with re-login work as a fallback.

**Independent Test**: Can be tested by obtaining a refresh token from login, then calling `POST /api/v1/auth/refresh` and verifying a new access token is returned.

**Affected Services**: Identity Service

**Domain Events Published**: None (token refresh does not represent a new authentication event)

**Events Consumed**: None

**API Endpoints**: `POST /api/v1/auth/refresh`

**Acceptance Scenarios**:

1. **Given** a valid refresh token, **When** `POST /api/v1/auth/refresh` is called, **Then**:
   - The system returns HTTP `200 OK` with a new access token and the same refresh token (rotation optional)
   - The new access token has a fresh expiry
   - The old access token is no longer valid

2. **Given** an expired or invalid refresh token, **When** `POST /api/v1/auth/refresh` is called, **Then**:
   - The system returns HTTP `401 Unauthorized` with error code `INVALID_REFRESH_TOKEN`
   - No new tokens are issued

---

### User Story 3 - Failed Authentication Tracking (Priority: P2)

As a security officer, I want the system to track failed authentication attempts and lock accounts after a threshold so that brute-force attacks are mitigated.

**Why this priority**: Account lockout is a critical security control that prevents credential stuffing and brute-force attacks. It is a regulatory requirement for financial platforms.

**Independent Test**: Can be tested by submitting consecutive failed login attempts and verifying the account is locked after the configured threshold.

**Affected Services**: Identity Service

**Domain Events Published**: None (account lockout is tracked internally)

**Events Consumed**: None

**API Endpoints**: `POST /api/v1/auth/login` (tracking within this endpoint)

**Acceptance Scenarios**:

1. **Given** a user account with 0 failed attempts, **When** 5 consecutive failed login attempts occur, **Then**:
   - After the 5th failure, the account status transitions to `LOCKED`
   - A `UserAccountLocked` event is published to Kafka topic `aegis.identity.user-account-locked`
   - Subsequent login attempts return `401 Unauthorized` with error code `ACCOUNT_LOCKED`

2. **Given** a user account that has been locked, **When** a successful login occurs after account unlock (future admin action), **Then**:
   - The failed attempt counter is reset to 0
   - The account status returns to `ACTIVE`

3. **Given** a successful login, **When** the authentication is validated, **Then**:
   - The failed attempt counter is reset to 0 (if previously non-zero)

---

### Edge Cases

- What happens when the JWT secret key is rotated? The system must support multiple active keys to validate existing tokens while issuing new ones with the current key.
- What happens when two concurrent login requests arrive for the same account? Race conditions on the failed attempt counter must be handled via optimistic locking on the user entity.
- What is the access token expiry? **Decision: 15 minutes**. Refresh token expiry: **7 days**.
- Should refresh tokens be stored server-side? **Decision: No.** Refresh tokens are self-contained JWTs with longer expiry. Server-side token storage is deferred to v2 if revocation requirements arise.
- How does the system handle brute-force attacks across distributed instances? Failed attempt counter is stored in the database (single source of truth), not in-memory.
- What happens when the user's account is in PENDING_VERIFICATION status? Authentication is allowed, but a warning header or specific response field can indicate verification is pending. **Decision: Authenticate normally but include a `emailVerified: false` field in the response.**
- Is there a maximum number of active sessions per user? **Decision: No limit for v1.** Multiple concurrent sessions are allowed.
- What is the minimum JWT signing algorithm? **Decision: RS256 (asymmetric) for production.** HS256 (symmetric) is acceptable for development.

---

## Requirements

### Functional Requirements

**Login Core**:

- **FR-001**: System MUST expose REST endpoint `POST /api/v1/auth/login` accepting a JSON body with fields: `email` (string, required), `password` (string, required)
- **FR-002**: System MUST validate credentials by comparing the provided password (BCrypt) against the stored password hash
- **FR-003**: System MUST return HTTP `200 OK` with JWT access token and refresh token on successful authentication
- **FR-004**: System MUST return HTTP `401 Unauthorized` with error code `INVALID_CREDENTIALS` for invalid email/password combinations
- **FR-005**: System MUST return HTTP `401 Unauthorized` with error code `ACCOUNT_LOCKED` if the account is in `LOCKED` status
- **FR-006**: System MUST return HTTP `401 Unauthorized` with error code `ACCOUNT_SUSPENDED` if the account is in `SUSPENDED` status
- **FR-007**: System MUST issue JWT access tokens with claims: `sub` (userId), `email`, `roles` (list), `iat` (issued at), `exp` (expiration)
- **FR-008**: System MUST issue refresh tokens as JWT with longer expiry (7 days) containing: `sub` (userId), `type: "refresh"`, `iat`, `exp`
- **FR-009**: Access token expiry MUST be 15 minutes
- **FR-010**: Refresh token expiry MUST be 7 days
- **FR-011**: System MUST expose REST endpoint `POST /api/v1/auth/refresh` accepting a refresh token and returning a new access token

**Failed Attempt Tracking**:

- **FR-012**: System MUST track consecutive failed authentication attempts per user account
- **FR-013**: System MUST lock the account after 5 consecutive failed attempts (status → `LOCKED`)
- **FR-014**: System MUST reset the failed attempt counter to 0 on successful authentication
- **FR-015**: System MUST publish domain event `UserAccountLocked` to Kafka topic `aegis.identity.user-account-locked` when an account is locked

**Event Publishing**:

- **FR-016**: System MUST publish domain event `UserAuthenticated` to Kafka topic `aegis.identity.user-authenticated` on every authentication attempt (success or failure)
- **FR-017**: The `UserAuthenticated` event payload MUST contain: `eventId` (UUID), `eventType` ("USER_AUTHENTICATED"), `userId`, `email`, `timestamp` (ISO 8601 UTC), `success` (boolean), `failureReason` (nullable string)
- **FR-018**: Failed authentication events MUST NOT include the plaintext password or password hash
- **FR-019**: System MUST use the transactional outbox pattern for event publishing

**Validation & Error Handling**:

- **FR-020**: System MUST NOT distinguish between "email not found" and "wrong password" in error responses (prevents email enumeration)
- **FR-021**: System MUST use constant-time comparison for password validation to prevent timing attacks
- **FR-022**: System MUST return the standard error response format: `{ "code", "message", "details", "timestamp" }`

**Security**:

- **FR-023**: The login endpoint MUST be marked `@PermitAll` (no authentication required)
- **FR-024**: System MUST sign JWT tokens with a secure key (RS256 in production, HS256 in dev)
- **FR-025**: System MUST validate JWT tokens on every authenticated request via a security filter
- **FR-026**: System MUST log all authentication attempts (success and failure) for security monitoring, excluding password data
- **FR-027**: System MUST NOT log, cache, or transmit the plaintext password beyond the initial request processing

**Persistence**:

- **FR-028**: System MUST add a `failed_login_attempts` column (INTEGER, DEFAULT 0) to the `users` table
- **FR-029**: System MUST add a `locked_until` column (TIMESTAMP, NULLABLE) to the `users` table for automatic lockout duration

### Key Entities

- **User** (Aggregate Root): Domain model in `com.aegis.identity.domain.model`. Extended with `failedLoginAttempts` (int) and `lockedUntil` (Instant). New state transitions: `ACTIVE` → `LOCKED` (on threshold exceeded).
- **TokenPair** (Value Object): Immutable value object (Java record) wrapping an access token and refresh token.
- **Credentials** (Value Object): Immutable value object (Java record) wrapping email and password for authentication commands.

### Domain Events

- **UserAuthenticated**: Published to Kafka topic `aegis.identity.user-authenticated`. Payload: `{ eventId, eventType, userId, email, timestamp, success, failureReason }`. Published on every authentication attempt.
- **UserAccountLocked**: Published to Kafka topic `aegis.identity.user-account-locked`. Payload: `{ eventId, eventType, userId, email, timestamp, failureCount }`. Published when an account is locked due to excessive failed attempts.

---

## Domain Model

### Extended Aggregate: User

```
User (Aggregate Root)
├── UserId              (Value Object - UUID v7)
├── Email               (Value Object - normalized, validated)
├── PasswordHash        (Value Object - BCrypt hash)
├── firstName           (String)
├── lastName            (String)
├── UserStatus          (Enum: PENDING_VERIFICATION | ACTIVE | LOCKED | SUSPENDED)
├── registeredAt        (Instant - UTC)
├── failedLoginAttempts (int)
├── lockedUntil         (Instant - nullable)
└── version             (Long - optimistic locking)
```

### State Machine (Extended for UC-002)

```
                    ┌──────────────────────────────────────────┐
                    │                                          │
                    ▼                                          │
PENDING_VERIFICATION ──→ ACTIVE ──→ LOCKED ──→ SUSPENDED       │
                              │         │                      │
                              │   (5 failed     (admin unlock) │
                              │    attempts)                   │
                              └─────────┘──────────────────────┘
```

### Value Objects (New)

| Value Object | Type | Description |
|-------------|------|-------------|
| `TokenPair` | `record TokenPair(String accessToken, String refreshToken)` | Wraps access and refresh JWT tokens. |
| `Credentials` | `record Credentials(String email, String password)` | Authentication credentials. |

---

## Business Rules

### Validation Rules

| Rule ID | Rule | Enforcement Point |
|---------|------|-------------------|
| BR-001 | Email MUST NOT be blank | Web layer (Jakarta validation) |
| BR-002 | Password MUST NOT be blank | Web layer (Jakarta validation) |
| BR-003 | Account MUST be in ACTIVE or PENDING_VERIFICATION status to authenticate | Domain layer (AuthenticateUserService) |
| BR-004 | Account MUST NOT be locked at time of authentication | Domain layer (AuthenticateUserService) |
| BR-005 | Account MUST NOT be suspended at time of authentication | Domain layer (AuthenticateUserService) |
| BR-006 | Failed login attempts MUST reset to 0 on successful authentication | Domain layer (AuthenticateUserService) |
| BR-007 | Account locks after 5 consecutive failed attempts | Domain layer (AuthenticateUserService) |

### Invariants

| Invariant ID | Invariant | Scope |
|--------------|-----------|-------|
| INV-001 | A User always has a non-negative `failedLoginAttempts` count | Aggregate lifetime |
| INV-002 | `UserStatus.LOCKED` implies `failedLoginAttempts >= 5` | State consistency |
| INV-003 | A locked user can only transition back to `ACTIVE` via admin action | State machine |

---

## Integration Model

### Synchronous Interactions

| Caller | Endpoint | Method | Purpose |
|--------|----------|--------|---------|
| Client | `/api/v1/auth/login` | POST | Submit credentials for authentication |
| Client | `/api/v1/auth/refresh` | POST | Refresh access token using refresh token |

### Asynchronous Interactions

| Producer | Topic | Consumers | Pattern |
|----------|-------|-----------|---------|
| Identity Service | `aegis.identity.user-authenticated` | Audit Service | Choreography (fire-and-forget) |
| Identity Service | `aegis.identity.user-account-locked` | Audit Service, Notification Service | Choreography (fire-and-forget) |

### Event Flow

```
Client ──POST /auth/login──→ API Gateway ──→ Identity Service
                                                  │
                                                  ├──→ [DB] Find user by email
                                                  ├──→ Verify password (BCrypt)
                                                  ├──→ Check account status
                                                  ├──→ Generate JWT tokens
                                                  ├──→ [DB] Update failedLoginAttempts
                                                  │
                                                  ├──→ Publish UserAuthenticated event
                                                  │    (via outbox)
                                                  │
                                                  └──→ Return 200 OK + TokenPair
```

## API Specification

### POST /api/v1/auth/login

**Description**: Authenticate a user with email and password.

**Authentication**: None (`@PermitAll`)

**Request Body**:

```json
{
  "email": "john.doe@example.com",
  "password": "SecureP@ss1"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| email | string | Yes | Valid email format |
| password | string | Yes | Non-blank |

**Success Response** (`200 OK`):

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "emailVerified": true
}
```

**Error Responses**:

| Status | Code | Scenario |
|--------|------|----------|
| `400 Bad Request` | `FIELD_REQUIRED` | Required field is missing or blank |
| `401 Unauthorized` | `INVALID_CREDENTIALS` | Invalid email/password combination |
| `401 Unauthorized` | `ACCOUNT_LOCKED` | Account is locked due to failed attempts |
| `401 Unauthorized` | `ACCOUNT_SUSPENDED` | Account is suspended |

### POST /api/v1/auth/refresh

**Description**: Refresh an expired access token using a valid refresh token.

**Authentication**: None

**Request Body**:

```json
{
  "refreshToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| refreshToken | string | Yes | Valid JWT refresh token |

**Success Response** (`200 OK`):

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Error Responses**:

| Status | Code | Scenario |
|--------|------|----------|
| `401 Unauthorized` | `INVALID_REFRESH_TOKEN` | Refresh token is expired, malformed, or invalid |

---

## Sequence Diagram

```
Client              API Gateway         Identity Service        PostgreSQL          Kafka           Audit Service
  │                     │                     │                    │                 │                   │
  │──POST /auth/login──→│                     │                    │                 │                   │
  │                     │──forward request───→│                    │                 │                   │
  │                     │                     │                    │                 │                   │
  │                     │                     │──findByEmail──────→│                 │                   │
  │                     │                     │←─user found────────│                 │                   │
  │                     │                     │                    │                 │                   │
  │                     │                     │──verify password───│                 │                   │
  │                     │                     │  (BCrypt matches)  │                 │                   │
  │                     │                     │                    │                 │                   │
  │                     │                     │──check status──────│                 │                   │
  │                     │                     │──generate tokens───│                 │                   │
  │                     │                     │                    │                 │                   │
  │                     │                     │──BEGIN TX─────────→│                 │                   │
  │                     │                     │──UPDATE user──────→│                 │                   │
  │                     │                     │  (reset fail count)│                 │                   │
  │                     │                     │──INSERT outbox────→│                 │                   │
  │                     │                     │──COMMIT TX────────→│                 │                   │
  │                     │                     │                    │                 │                   │
  │                     │                     │                    │                 │                   │
  │                     │                     │  (outbox relay - async)             │                   │
  │                     │                     │──────────────────────────────────────→│                   │
  │                     │                     │                    │                 │──UserAuthenticated→│
  │                     │                     │                    │                 │                   │──persist audit
  │                     │                     │                    │                 │                   │  record
  │                     │                     │                    │                 │                   │
  │←──200 OK + tokens───│←──200 OK + tokens───│                    │                 │                   │
  │                     │                     │                    │                 │                   │
```

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: A user can authenticate in a single API call with response time under 300ms (p95)
- **SC-002**: The system correctly rejects all invalid authentication attempts with appropriate HTTP status codes
- **SC-003**: 100% of authentication attempts (success and failure) produce a `UserAuthenticated` event in Kafka
- **SC-004**: Accounts are locked after 5 consecutive failed attempts
- **SC-005**: Duplicate concurrent authentication attempts are handled consistently (no race condition on failed attempt counter)
- **SC-006**: Passwords are never stored in plaintext and never appear in logs, API responses, or error messages
- **SC-007**: Identity Service unit tests achieve 100% coverage on domain logic (authentication flow, account lockout, token validation)
- **SC-008**: Integration tests verify the full authentication flow including database persistence, event publishing, and JWT token validation

---

## Assumptions

- Users exist in the system (created via UC-001 User Registration) before attempting authentication
- The API Gateway handles TLS termination, rate limiting, and request correlation ID injection before forwarding to Identity Service
- Kafka is available as the event bus; the transactional outbox pattern handles temporary Kafka unavailability
- Audit Service is independently deployable and consumes events asynchronously — it does not block the authentication response
- PostgreSQL is the persistence store for the Identity Service with its own isolated schema
- The JWT signing key is configured via environment variable or secret management, not hardcoded
- Account unlock is an admin action (out of scope for UC-002)
- Refresh token rotation (issuing a new refresh token on each refresh) is deferred to v2
- Rate limiting for login attempts is configured at the API Gateway level: 20 requests per minute per IP address
