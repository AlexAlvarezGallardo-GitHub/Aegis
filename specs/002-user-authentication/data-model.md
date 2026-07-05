# Data Model: UC-002 User Authentication

**Branch**: `feature/uc-002-user-authentication` | **Date**: 2026-07-03

---

## Domain Model (Extended)

### Aggregate Root: User (Extended)

```
com.aegis.identity.domain.model.User
├── userId: UserId (Value Object)
├── email: Email (Value Object)
├── passwordHash: PasswordHash (Value Object)
├── firstName: String
├── lastName: String
├── status: UserStatus (Enum)
├── registeredAt: Instant
├── failedLoginAttempts: int
├── lockedUntil: Instant (nullable)
├── updatedAt: Instant
└── version: Long (optimistic locking)
```

### New Value Objects

| Value Object | Type | Description |
|-------------|------|-------------|
| `TokenPair` | `record TokenPair(String accessToken, String refreshToken)` | Wraps access and refresh JWT tokens. |
| `Credentials` | `record Credentials(String email, String password)` | Authentication credentials value object. |

### New Domain Events

| Event | Type | Payload |
|-------|------|---------|
| `UserAuthenticated` | `record` | `eventId: UUID, eventType: String, userId: UUID, email: String, timestamp: Instant, success: boolean, failureReason: String, correlationId: String` |
| `UserAccountLocked` | `record` | `eventId: UUID, eventType: String, userId: UUID, email: String, timestamp: Instant, failureCount: int, correlationId: String` |

### New Exceptions

| Exception | Error Code | When Thrown |
|-----------|------------|-------------|
| `InvalidCredentialsException` | `INVALID_CREDENTIALS` | Email not found or password doesn't match |
| `AccountLockedException` | `ACCOUNT_LOCKED` | User account is in LOCKED status |
| `AccountSuspendedException` | `ACCOUNT_SUSPENDED` | User account is in SUSPENDED status |

---

## Persistence Model (Changes)

### Extended Table: `users` (new columns)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `failed_login_attempts` | `INTEGER` | `NOT NULL`, `DEFAULT 0` | Consecutive failed login count |
| `locked_until` | `TIMESTAMP WITH TIME ZONE` | `NULL` | Auto-lock expiry (null = not locked) |

### Complete `users` Table (after V2 migration)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | `PRIMARY KEY` | User ID (UUID v7) |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Normalized email address |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | BCrypt hash |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | User's first name |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | User's last name |
| `status` | `VARCHAR(30)` | `NOT NULL` | User status enum value |
| `failed_login_attempts` | `INTEGER` | `NOT NULL`, `DEFAULT 0` | Consecutive failed login count |
| `locked_until` | `TIMESTAMP WITH TIME ZONE` | `NULL` | Auto-lock expiry |
| `registered_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Registration timestamp (UTC) |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Last update timestamp (UTC) |
| `version` | `BIGINT` | `NOT NULL`, `DEFAULT 0` | Optimistic locking version |

---

## DDL (PostgreSQL) — V2 Migration

```sql
ALTER TABLE users
  ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
```

---

## Domain-to-JPA Mapping (New Fields)

| Domain Concept | JPA Representation |
|---------------|-------------------|
| `User.failedLoginAttempts` | `UserJpaEntity.failedLoginAttempts` |
| `User.lockedUntil` | `UserJpaEntity.lockedUntil` |

---

## Flyway Migration

Migration file: `V2__add_auth_fields_to_users.sql`

Location: `aegis-identity-service/src/main/resources/db/migration/`
