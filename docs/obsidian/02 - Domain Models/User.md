---
type: domain-model
service: aegis-identity-service
layer: domain
tags: [ddd, aggregate, identity]
status: implemented
---

# User

Aggregate root for the Identity bounded context.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | [[02 - Domain Models/UserId\|UserId]] | Unique identifier |
| `email` | [[02 - Domain Models/Email\|Email]] | Validated email address |
| `passwordHash` | [[02 - Domain Models/PasswordHash\|PasswordHash]] | BCrypt hash |
| `firstName` | String | First name |
| `lastName` | String | Last name |
| `status` | [[02 - Domain Models/UserStatus\|UserStatus]] | Account state |
| `failedLoginAttempts` | int | Counter for lockout |
| `lockedUntil` | Instant | Lockout expiration |
| `registeredAt` | Instant | Registration timestamp |
| `updatedAt` | Instant | Last update timestamp |
| `version` | long | Optimistic locking version |

## Factory Methods

- `User.register(rawEmail, rawPassword, firstName, lastName, hasher)` → User with PENDING_VERIFICATION status (validates raw password via [[02 - Domain Models/Password|Password]])
- `User.rehydrate(userId, email, passwordHash, firstName, lastName, status, failedLoginAttempts, lockedUntil, registeredAt, updatedAt, version)` → reconstituted aggregate

## Business Methods

- `authenticate(rawPassword, hasher, correlationId)` → returns [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] with `success` flag; locks after 5 failed attempts (15 min)
- `toRegisteredEvent(correlationId)` → [[03 - Domain Events/UserRegistered\|UserRegistered]]
- `toAccountLockedEvent(correlationId)` → [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]]
- `isLockedDueToFailures()` → true when status is LOCKED from failed attempts

## Domain Events Published

- [[03 - Domain Events/UserRegistered\|UserRegistered]] (on register)
- [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] (on login success AND failure, `success` flag)
- [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] (on 5th failed attempt)

## Relationships

- Contains: [[02 - Domain Models/UserId\|UserId]], [[02 - Domain Models/Email\|Email]], [[02 - Domain Models/PasswordHash\|PasswordHash]], [[02 - Domain Models/UserStatus\|UserStatus]]
- Consumed by: [[04 - Ports/outbound/UserRepository\|UserRepository]], [[04 - Ports/outbound/PasswordHasher\|PasswordHasher]]
- Mapped to: `UserJpaEntity` in infrastructure

## Business Rules

- Max 5 failed attempts before lockout (900 seconds)
- Cannot authenticate if LOCKED (and not expired) or SUSPENDED
- Raw passwords validated via [[02 - Domain Models/Password|Password]] before hashing
