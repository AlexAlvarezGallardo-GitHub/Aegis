# Feature Specification: UC-001 User Registration

**Feature Branch**: `feature/uc-001-user-registration`

**Created**: 2026-06-28

**Status**: Draft

**Input**: User description: "User Registration — End user self-registers on the Aegis platform by providing email, password, and personal information. Identity Service validates the data, creates the user account, publishes domain events for audit and notification services."

---

## Affected Bounded Contexts

| Service | Role | Interaction |
|---------|------|-------------|
| **Identity Service** | Primary owner | Receives registration request, validates data, creates account, publishes events |
| **Audit Service** | Event consumer | Receives `UserRegistered` event, persists immutable audit record |
| **Notification Service** | Event consumer | Receives `UserRegistered` event, sends welcome email |

---

## User Scenarios & Testing

### User Story 1 - Self-Registration with Email and Password (Priority: P1)

As a new end user, I want to register on the Aegis platform by providing my email, password, and personal details so that I can access the digital wallet and payment features.

**Why this priority**: This is the entry point to the entire platform. Without user registration, no other use case (wallet creation, payments, transfers) can occur. It is the foundational identity operation.

**Independent Test**: Can be fully tested by submitting a valid registration request via `POST /api/v1/users/register` and verifying the account is created, the `UserRegistered` event is published, and the user transitions to `PENDING_VERIFICATION` state.

**Affected Services**: Identity Service

**Domain Events Published**: `aegis.identity.user-registered`

**Events Consumed**: None (entry-point use case)

**API Endpoints**: `POST /api/v1/users/register`

**Acceptance Scenarios**:

1. **Given** no user exists with email `john@example.com`, **When** a valid registration request is submitted with email, password, first name, and last name, **Then**:
   - A new user account is created with status `PENDING_VERIFICATION`
   - The system returns HTTP `201 Created` with the user's ID and status
   - Event `aegis.identity.user-registered` is published with payload: `{ userId, email, firstName, lastName, registeredAt }`
   - Audit Service receives the event and persists an immutable audit record
   - Notification Service receives the event and queues a welcome email

2. **Given** a user already exists with email `john@example.com`, **When** a registration request is submitted with the same email, **Then**:
   - The system returns HTTP `409 Conflict` with standard error response `{ code: "EMAIL_ALREADY_REGISTERED", message: "...", details: null, timestamp: "..." }`
   - No event is published
   - No audit record is created for the duplicate attempt (to prevent email enumeration via audit logs)

3. **Given** the registration request contains an invalid email format, **When** the request is submitted, **Then**:
   - The system returns HTTP `400 Bad Request` with validation error details
   - No account is created
   - No event is published

4. **Given** the registration request contains a password that does not meet complexity requirements, **When** the request is submitted, **Then**:
   - The system returns HTTP `400 Bad Request` with specific password policy violation details
   - No account is created

5. **Given** the registration request is missing required fields (first name or last name), **When** the request is submitted, **Then**:
   - The system returns HTTP `400 Bad Request` with field-level validation errors
   - No account is created

---

### User Story 2 - Email Format and Uniqueness Validation (Priority: P2)

As the platform operator, I want all registration emails to be validated for correct format and global uniqueness so that each user account has a verified, unique email address that serves as their login identifier.

**Why this priority**: Email is the primary user identifier across the platform. Invalid or duplicate emails would break authentication, notifications, and cross-service user resolution. This is a critical data integrity guard.

**Independent Test**: Can be tested by submitting registrations with various email formats (valid, malformed, with special characters, with whitespace) and duplicate emails, verifying correct validation responses.

**Affected Services**: Identity Service

**Domain Events Published**: None (validation-only concern)

**Events Consumed**: None

**API Endpoints**: `POST /api/v1/users/register` (validation layer within this endpoint)

**Acceptance Scenarios**:

1. **Given** a registration request with email `user@domain.com`, **When** the email passes format validation, **Then** the system proceeds to uniqueness check
2. **Given** a registration request with email `not-an-email`, **When** format validation runs, **Then** the system returns `400 Bad Request` with error code `INVALID_EMAIL_FORMAT`
3. **Given** a registration request with email containing leading/trailing whitespace ` user@domain.com `, **When** validation runs, **Then** the system normalizes the email (trims and lowercases) before validation
4. **Given** a registration request with a valid but already-registered email, **When** uniqueness check runs, **Then** the system returns `409 Conflict` with error code `EMAIL_ALREADY_REGISTERED`

---

### User Story 3 - Password Security Enforcement (Priority: P2)

As the platform security officer, I want all registration passwords to meet minimum complexity requirements so that user accounts are protected against brute-force and credential-stuffing attacks.

**Why this priority**: Password security is a regulatory and security requirement. Weak passwords compromise user funds and platform trust. This is co-equal with email validation in priority but listed separately for clarity.

**Independent Test**: Can be tested by submitting registrations with passwords of varying strength and verifying acceptance/rejection per the password policy.

**Affected Services**: Identity Service

**Domain Events Published**: None (validation-only concern)

**Events Consumed**: None

**API Endpoints**: `POST /api/v1/users/register` (validation layer within this endpoint)

**Acceptance Scenarios**:

1. **Given** a password with 12+ characters including uppercase, lowercase, digit, and special character, **When** validation runs, **Then** the password is accepted
2. **Given** a password with fewer than 8 characters, **When** validation runs, **Then** the system returns `400 Bad Request` with error code `PASSWORD_TOO_SHORT` and message indicating minimum 8 characters
3. **Given** a password without at least one uppercase letter, **When** validation runs, **Then** the system returns `400 Bad Request` with error code `PASSWORD_MISSING_UPPERCASE`
4. **Given** a password without at least one digit, **When** validation runs, **Then** the system returns `400 Bad Request` with error code `PASSWORD_MISSING_DIGIT`
5. **Given** a password without at least one special character, **When** validation runs, **Then** the system returns `400 Bad Request` with error code `PASSWORD_MISSING_SPECIAL_CHARACTER`
6. **Given** any valid password, **When** the account is created, **Then** the password is stored as a one-way hash (BCrypt or equivalent) and the original plaintext is never persisted or logged

---

### User Story 4 - Registration Audit Trail (Priority: P2)

As a compliance officer, I want every successful user registration to generate an immutable audit record so that the platform maintains a verifiable trail of all identity creation events for regulatory compliance.

**Why this priority**: Audit trails are a regulatory requirement for financial platforms. Every identity lifecycle event must be traceable. This is essential for SOC2, PCI-DSS, and AML compliance demonstrations.

**Independent Test**: Can be tested by registering a user and verifying that an audit record exists in the Audit Service with the correct event type, timestamp, and user details.

**Affected Services**: Identity Service (publisher), Audit Service (consumer)

**Domain Events Published**: `aegis.identity.user-registered`

**Events Consumed**: Audit Service consumes `aegis.identity.user-registered`

**API Endpoints**: None (event-driven interaction)

**Acceptance Scenarios**:

1. **Given** a successful user registration, **When** the `UserRegistered` event is published to Kafka topic `aegis.identity.user-registered`, **Then** Audit Service consumes the event within 5 seconds and persists an immutable audit record containing: `{ auditId, eventType: "USER_REGISTERED", userId, email, timestamp, correlationId, source: "identity-service" }`
2. **Given** the Audit Service Kafka consumer is temporarily unavailable, **When** a `UserRegistered` event is published, **Then** the event remains in the Kafka topic and Audit Service processes it upon recovery (no data loss)
3. **Given** a duplicate `UserRegistered` event (Kafka redelivery), **When** Audit Service processes it, **Then** the system detects the duplicate via `eventId` and does not create a second audit record (idempotent consumption)

---

### User Story 5 - Welcome Notification (Priority: P3)

As a newly registered user, I want to receive a welcome email confirming my registration so that I have confirmation that my account was created successfully.

**Why this priority**: Welcome notifications improve user experience and confirm successful registration, but the platform is functional without them. This can be deferred after core registration and audit are working.

**Independent Test**: Can be tested by registering a user and verifying that Notification Service dispatches a welcome email to the registered address.

**Affected Services**: Identity Service (publisher), Notification Service (consumer)

**Domain Events Published**: `aegis.identity.user-registered`

**Events Consumed**: Notification Service consumes `aegis.identity.user-registered`

**API Endpoints**: None (event-driven interaction)

**Acceptance Scenarios**:

1. **Given** a successful user registration, **When** the `UserRegistered` event is published, **Then** Notification Service consumes the event and dispatches a welcome email to the user's registered email address using the `WELCOME_EMAIL` template
2. **Given** the email delivery fails (SMTP error), **When** Notification Service attempts to send, **Then** a `NotificationFailed` event is published with the failure reason and the notification is queued for retry
3. **Given** a duplicate `UserRegistered` event, **When** Notification Service processes it, **Then** the system detects the duplicate and does not send a second welcome email (idempotent consumption)

---

### Edge Cases

- What happens when two concurrent registration requests arrive with the same email? The system MUST handle this via database unique constraint on email, returning `409 Conflict` for the second request regardless of timing.
- What happens when Kafka is unavailable during event publishing after successful account creation? The system MUST use the transactional outbox pattern to ensure events are eventually published. The account is created first, and the event is persisted to an outbox table within the same database transaction, then asynchronously forwarded to Kafka.
- What happens when the user submits a registration with an email from a disposable email provider? **Decision: Deferred to v2.** UC-001 accepts any valid RFC 5322 email. Disposable domain blocking can be added as a policy rule later without changing the API contract.
- How does the system handle registration rate limiting to prevent abuse? The API Gateway MUST enforce rate limiting on the registration endpoint. **Decision: 10 requests per minute per IP address.** This is configured at the Gateway level, not within Identity Service.
- What is the maximum password length? **Decision: 128 characters maximum.** BCrypt truncates input at 72 bytes, so passwords longer than 128 characters are rejected to prevent computational DoS attacks with excessively long inputs.
- Does the registration support optional fields (phone, date of birth)? **Decision: No.** Registration is minimal (email, password, firstName, lastName). Additional profile fields are added via a separate profile update use case.
- Is username-based login supported? **Decision: No.** Email is the sole login identifier. This simplifies the identity model and avoids username collision issues.
- What happens when the password is sent in plaintext over the wire? The system MUST enforce TLS for all API communication. Passwords are hashed server-side only after receipt.

---

## Requirements

### Functional Requirements

**Registration Core**:

- **FR-001**: System MUST expose REST endpoint `POST /api/v1/users/register` accepting a JSON body with fields: `email` (string, required), `password` (string, required), `firstName` (string, required), `lastName` (string, required)
- **FR-002**: System MUST validate that `email` conforms to RFC 5322 format after normalization (trim whitespace, lowercase)
- **FR-003**: System MUST enforce global email uniqueness across all user accounts
- **FR-004**: System MUST enforce password complexity: minimum 8 characters, maximum 128 characters, at least 1 uppercase letter, 1 lowercase letter, 1 digit, 1 special character
- **FR-005**: System MUST hash the password using BCrypt (strength >= 10) before persistence. Plaintext passwords MUST NEVER be stored or logged
- **FR-006**: System MUST create the user account with initial status `PENDING_VERIFICATION`
- **FR-007**: System MUST assign a globally unique `UserId` (UUID v7) to each new account
- **FR-008**: System MUST record `registeredAt` timestamp (UTC) at the moment of account creation
- **FR-009**: System MUST return HTTP `201 Created` with response body containing `userId`, `email`, `status`, and `registeredAt` on successful registration
- **FR-010**: System MUST NOT return the hashed password or any internal identifiers in the API response

**Event Publishing**:

- **FR-011**: System MUST publish domain event `UserRegistered` to Kafka topic `aegis.identity.user-registered` upon successful account creation
- **FR-012**: The `UserRegistered` event payload MUST contain: `eventId` (UUID), `eventType` ("USER_REGISTERED"), `userId`, `email`, `firstName`, `lastName`, `registeredAt` (ISO 8601 UTC), `correlationId`
- **FR-013**: System MUST use the transactional outbox pattern to guarantee at-least-once event delivery even if Kafka is temporarily unavailable
- **FR-014**: Each event MUST include a unique `eventId` to support idempotent consumption by downstream services

**Validation & Error Handling**:

- **FR-015**: System MUST validate all request inputs using `@Validated` and return the standard error response format: `{ "code", "message", "details", "timestamp" }`
- **FR-016**: System MUST return HTTP `400 Bad Request` for validation failures with field-specific error details
- **FR-017**: System MUST return HTTP `409 Conflict` for duplicate email registration
- **FR-018**: System MUST NOT expose whether an email is already registered to unauthenticated callers beyond the `409` response (no email enumeration via timing attacks or error message differences)

**Security**:

- **FR-019**: The registration endpoint MUST be marked `@PermitAll` (no authentication required) as it is the account creation entry point
- **FR-020**: System MUST enforce TLS 1.2+ for all API communication
- **FR-021**: System MUST log all registration attempts (success and failure) for security monitoring, excluding password data
- **FR-022**: System MUST NOT log, cache, or transmit the plaintext password beyond the initial request processing

**Persistence**:

- **FR-023**: System MUST persist user accounts in PostgreSQL with `snake_case` table naming (`users` table in Identity Service schema)
- **FR-024**: System MUST enforce email uniqueness via a database unique constraint (not only application-level check)

### Key Entities

- **User** (Aggregate Root): Domain model in `com.aegis.identity.domain.model`. The central aggregate representing a registered platform user. Attributes: `userId` (UserId value object), `email` (Email value object), `passwordHash` (PasswordHash value object), `firstName` (string), `lastName` (string), `status` (UserStatus enum), `registeredAt` (Instant). State transitions: `PENDING_VERIFICATION` -> `ACTIVE` (on email verification, future UC). Published events: `UserRegistered`.

- **UserId** (Value Object): Immutable value object (Java record) wrapping a UUID v7. Provides type safety and prevents mixing user identifiers with other UUIDs in the system.

- **Email** (Value Object): Immutable value object (Java record) encapsulating email normalization (trim, lowercase) and format validation at construction time. A valid Email instance is guaranteed to be well-formed.

- **PasswordHash** (Value Object): Immutable value object (Java record) wrapping a BCrypt hash string. Constructed only from a plaintext password via a factory method that performs hashing. The plaintext is never accessible from this object.

- **UserStatus** (Enum): Defines the lifecycle states of a user account. Initial values for this feature: `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `SUSPENDED`. Only `PENDING_VERIFICATION` is reachable via this use case.

- **UserRegistered** (Domain Event): Published to Kafka topic `aegis.identity.user-registered`. Payload: `{ eventId: UUID, eventType: String, userId: UUID, email: String, firstName: String, lastName: String, registeredAt: Instant, correlationId: String }`.

---

## Domain Model

### Aggregate: User

```
User (Aggregate Root)
├── UserId          (Value Object - UUID v7)
├── Email           (Value Object - normalized, validated)
├── PasswordHash    (Value Object - BCrypt hash)
├── firstName       (String)
├── lastName        (String)
├── UserStatus      (Enum: PENDING_VERIFICATION | ACTIVE | LOCKED | SUSPENDED)
└── registeredAt    (Instant - UTC)
```

### State Machine (User Lifecycle - Partial for UC-001)

```
[Registration Request]
        │
        ▼
PENDING_VERIFICATION  ←── (UC-001 creates user in this state)
        │
        ├──→ ACTIVE          (future: email verification - UC-002)
        ├──→ LOCKED          (future: too many failed auth attempts)
        └──→ SUSPENDED       (future: admin action)
```

---

## Business Rules

### Validation Rules

| Rule ID | Rule | Enforcement Point |
|---------|------|-------------------|
| BR-001 | Email MUST be valid RFC 5322 format | Domain layer (Email value object constructor) |
| BR-002 | Email MUST be globally unique | Domain layer (repository check) + Infrastructure layer (DB unique constraint) |
| BR-003 | Password MUST be >= 8 characters | Domain layer (Password validation policy) |
| BR-003b | Password MUST be <= 128 characters | Domain layer (Password validation policy) |
| BR-004 | Password MUST contain >= 1 uppercase letter | Domain layer (Password validation policy) |
| BR-005 | Password MUST contain >= 1 lowercase letter | Domain layer (Password validation policy) |
| BR-006 | Password MUST contain >= 1 digit | Domain layer (Password validation policy) |
| BR-007 | Password MUST contain >= 1 special character | Domain layer (Password validation policy) |
| BR-008 | First name MUST NOT be blank | Domain layer (User factory method) |
| BR-009 | Last name MUST NOT be blank | Domain layer (User factory method) |

### Invariants

| Invariant ID | Invariant | Scope |
|--------------|-----------|-------|
| INV-001 | A User always has a valid, non-null UserId | Aggregate lifetime |
| INV-002 | A User always has a valid, normalized Email | Aggregate lifetime |
| INV-003 | A User always has a non-null PasswordHash (plaintext never accessible) | Aggregate lifetime |
| INV-004 | A User's status transitions are unidirectional and follow the state machine | State changes |
| INV-005 | A User's `registeredAt` timestamp is immutable once set | Aggregate lifetime |

### Policies

| Policy ID | Policy |
|-----------|--------|
| POL-001 | New registrations start in `PENDING_VERIFICATION` status |
| POL-002 | Passwords are hashed with BCrypt (strength >= 10) before persistence |
| POL-003 | Events are published using the transactional outbox pattern |
| POL-004 | All events include a unique `eventId` for idempotent consumption |
| POL-005 | Registration endpoint is unauthenticated (`@PermitAll`) |

---

## Domain Events

### Events Published

| Event | Topic | Trigger | Payload |
|-------|-------|---------|---------|
| `UserRegistered` | `aegis.identity.user-registered` | Successful account creation | See below |

**UserRegistered Payload**:

```json
{
  "eventId": "01912345-6789-7abc-def0-123456789abc",
  "eventType": "USER_REGISTERED",
  "userId": "01912345-6789-7abc-def0-123456789abc",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "registeredAt": "2026-06-28T14:30:00.000Z",
  "correlationId": "req-abc-123"
}
```

### Events Consumed

None. This use case is the platform entry point.

### Event Consumers (Downstream)

| Consumer Service | Event | Purpose |
|-----------------|-------|---------|
| Audit Service | `UserRegistered` | Persist immutable audit record |
| Notification Service | `UserRegistered` | Send welcome email |

---

## Integration Model

### Synchronous Interactions

| Caller | Endpoint | Method | Purpose |
|--------|----------|--------|---------|
| API Gateway / Client | `/api/v1/users/register` | POST | Submit registration request |

### Asynchronous Interactions

| Producer | Topic | Consumers | Pattern |
|----------|-------|-----------|---------|
| Identity Service | `aegis.identity.user-registered` | Audit Service, Notification Service | Choreography (fire-and-forget) |

### Event Flow

```
Client ──POST──→ API Gateway ──→ Identity Service
                                      │
                                      ├──→ [DB] Create user account
                                      ├──→ [DB] Write to outbox table (same transaction)
                                      │
                                      ▼
                                 Outbox Relay ──→ Kafka: aegis.identity.user-registered
                                                      │
                                          ┌───────────┼───────────┐
                                          ▼           ▼           ▼
                                    Audit Service   Notification  (future
                                     (persist       Service       consumers)
                                      audit record)  (send email)
```

---

## API Specification

### POST /api/v1/users/register

**Description**: Register a new user account on the Aegis platform.

**Authentication**: None (`@PermitAll`)

**Request Body**:

```json
{
  "email": "john.doe@example.com",
  "password": "SecureP@ss1",
  "firstName": "John",
  "lastName": "Doe"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| email | string | Yes | Valid RFC 5322 email, max 255 chars |
| password | string | Yes | Min 8 chars, max 128 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char |
| firstName | string | Yes | Non-blank, max 100 chars |
| lastName | string | Yes | Non-blank, max 100 chars |

**Success Response** (`201 Created`):

```json
{
  "userId": "01912345-6789-7abc-def0-123456789abc",
  "email": "john.doe@example.com",
  "status": "PENDING_VERIFICATION",
  "registeredAt": "2026-06-28T14:30:00.000Z"
}
```

**Error Responses**:

| Status | Code | Scenario |
|--------|------|----------|
| `400 Bad Request` | `INVALID_EMAIL_FORMAT` | Email does not match RFC 5322 format |
| `400 Bad Request` | `PASSWORD_TOO_SHORT` | Password is fewer than 8 characters |
| `400 Bad Request` | `PASSWORD_TOO_LONG` | Password exceeds 128 characters |
| `400 Bad Request` | `PASSWORD_MISSING_UPPERCASE` | Password lacks uppercase letter |
| `400 Bad Request` | `PASSWORD_MISSING_LOWERCASE` | Password lacks lowercase letter |
| `400 Bad Request` | `PASSWORD_MISSING_DIGIT` | Password lacks digit |
| `400 Bad Request` | `PASSWORD_MISSING_SPECIAL_CHARACTER` | Password lacks special character |
| `400 Bad Request` | `FIELD_REQUIRED` | Required field is missing or blank |
| `409 Conflict` | `EMAIL_ALREADY_REGISTERED` | Email is already associated with an account |

**Error Response Format** (standard Aegis error contract):

```json
{
  "code": "EMAIL_ALREADY_REGISTERED",
  "message": "An account with this email address already exists.",
  "details": null,
  "timestamp": "2026-06-28T14:30:00.000Z"
}
```

---

## Sequence Diagram

```
Client              API Gateway         Identity Service        PostgreSQL          Kafka           Audit Service      Notification Service
  │                     │                     │                    │                 │                   │                    │
  │──POST /register────→│                     │                    │                 │                   │                    │
  │                     │──forward request───→│                    │                 │                   │                    │
  │                     │                     │                    │                 │                   │                    │
  │                     │                     │──validate email───→│                 │                   │                    │
  │                     │                     │←─email not found───│                 │                   │                    │
  │                     │                     │                    │                 │                   │                    │
  │                     │                     │──validate password─│                 │                   │                    │
  │                     │                     │  (in-memory)       │                 │                   │                    │
  │                     │                     │                    │                 │                   │                    │
  │                     │                     │──BEGIN TX─────────→│                 │                   │                    │
  │                     │                     │──INSERT user──────→│                 │                   │                    │
  │                     │                     │──INSERT outbox────→│                 │                   │                    │
  │                     │                     │──COMMIT TX────────→│                 │                   │                    │
  │                     │                     │                    │                 │                   │                    │
  │                     │                     │                    │                 │                   │                    │
  │                     │                     │   (outbox relay - async)             │                   │                    │
  │                     │                     │──────────────────────────────────────→│                   │                    │
  │                     │                     │                    │                 │──UserRegistered──→│                    │
  │                     │                     │                    │                 │──UserRegistered──────────────────────→│
  │                     │                     │                    │                 │                   │                    │
  │                     │                     │                    │                 │                   │──persist audit     │
  │                     │                     │                    │                 │                   │  record            │
  │                     │                     │                    │                 │                   │                    │──send welcome
  │                     │                     │                    │                 │                   │                    │  email
  │                     │                     │                    │                 │                   │                    │
  │←──201 Created───────│←──201 Created───────│                    │                 │                   │                    │
  │                     │                     │                    │                 │                   │                    │
```

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: A user can complete registration in a single API call with response time under 500ms (p95)
- **SC-002**: The system correctly rejects all invalid registration requests with appropriate HTTP status codes and error codes
- **SC-003**: 100% of successful registrations produce a `UserRegistered` event in Kafka (guaranteed via outbox pattern)
- **SC-004**: Audit Service persists an audit record for 100% of `UserRegistered` events within 30 seconds of publication
- **SC-005**: Notification Service dispatches a welcome email for 100% of `UserRegistered` events within 60 seconds of publication
- **SC-006**: Duplicate email registration attempts return `409 Conflict` with no side effects (no events, no partial data)
- **SC-007**: Passwords are never stored in plaintext and never appear in logs, API responses, or error messages
- **SC-008**: Identity Service unit tests achieve 100% coverage on domain logic (User aggregate, value objects, validation policies)
- **SC-009**: Integration tests verify the full registration flow including database persistence, event publishing, and downstream consumption
- **SC-010**: All code follows hexagonal architecture with zero infrastructure types in the domain layer

---

## Assumptions

- Users have access to a valid email address at the time of registration
- Email verification (clicking a verification link) is a separate use case (future UC) and not part of UC-001. Users are created in `PENDING_VERIFICATION` state
- The API Gateway handles TLS termination, rate limiting, and request correlation ID injection before forwarding to Identity Service
- Kafka is available as the event bus; the transactional outbox pattern handles temporary Kafka unavailability
- Audit Service and Notification Service are independently deployable and consume events asynchronously — they do not block the registration response
- PostgreSQL is the persistence store for the Identity Service with its own isolated schema
- No third-party identity providers (OAuth2 social login) are in scope for UC-001 — this is email/password registration only
- Disposable email domain blocking is deferred to v2 (not in scope for UC-001)
- Rate limiting is configured at the API Gateway level: 10 requests per minute per IP address
