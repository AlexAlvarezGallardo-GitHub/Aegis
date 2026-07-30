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
| `status` | [[02 - Domain Models/UserStatus\|UserStatus]] | Account state |
| `failedLoginAttempts` | int | Counter for lockout |
| `lockedUntil` | Instant | Lockout expiration |
| `createdAt` | Instant | Registration timestamp |

## Factory Methods

- `User.register(email, passwordHash)` → User with PENDING_VERIFICATION status

## Business Methods

- `authenticate(passwordHash)` → void (throws on invalid/locked/suspended)
- `recordFailedAttempt()` → increments counter, locks after 5
- `resetFailedAttempts()` → resets counter, clears lock
- `lockAccount()` → sets LOCKED status

## Domain Events Published

- [[03 - Domain Events/UserRegistered\|UserRegistered]] (on register)
- [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] (on login success)
- [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] (on 5th failed attempt)

## Relationships

- Contains: [[02 - Domain Models/UserId\|UserId]], [[02 - Domain Models/Email\|Email]], [[02 - Domain Models/PasswordHash\|PasswordHash]], [[02 - Domain Models/UserStatus\|UserStatus]]
- Consumed by: [[04 - Ports/outbound/UserRepository\|UserRepository]], [[04 - Ports/outbound/PasswordHasher\|PasswordHasher]]
- Mapped to: `UserJpaEntity` in infrastructure

## Business Rules

- Max 5 failed attempts before lockout
- Lockout duration: configurable (default 30 min)
- Cannot authenticate if LOCKED or SUSPENDED
