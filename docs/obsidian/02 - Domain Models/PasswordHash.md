---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, security]
status: implemented
---

# PasswordHash

Value object wrapping a hashed password. The BCrypt hash is produced by the [[04 - Ports/outbound/PasswordHasher|PasswordHasher]] adapter.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `hash` | String | BCrypt hash (e.g., `$2a$10$...`) |

## Validation

- Non-null
- Non-blank
- `toString()` returns `[PROTECTED]` to avoid leaking the hash in logs

## Factory Methods

- `PasswordHash.of(hash)` → validated instance

## Used By

- [[02 - Domain Models/User|User]] aggregate root
- [[02 - Domain Models/Password|Password]] (validated plain-text source)
- [[04 - Ports/outbound/PasswordHasher|PasswordHasher]] port (creation + verification)

## JPA Mapping

Stored as `VARCHAR(60)` in `users.password_hash` column.
