---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, auth, security]
status: implemented
---

# Password

Value object for a validated plain-text user password. Never persisted — only the BCrypt [[02 - Domain Models/PasswordHash|PasswordHash]] is stored.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `value` | String | Validated plain-text password |

## Validation Rules

- Length between 8 and 128 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special (non-letter, non-digit) character
- Throws `WeakPasswordException` with a specific code: `PASSWORD_TOO_SHORT`, `PASSWORD_TOO_LONG`, `PASSWORD_MISSING_UPPERCASE`, `PASSWORD_MISSING_LOWERCASE`, `PASSWORD_MISSING_DIGIT`, `PASSWORD_MISSING_SPECIAL_CHARACTER`

## Factory Methods

- `Password.of(rawPassword)` → validated instance or `WeakPasswordException`

## Used By

- [[02 - Domain Models/User|User]] aggregate root (`User.register`) before hashing
- [[04 - Ports/outbound/PasswordHasher|PasswordHasher]] receives the validated plain-text value

## JPA Mapping

Not persisted. Only the hashed form [[02 - Domain Models/PasswordHash|PasswordHash]] is stored in `users.password_hash`.
