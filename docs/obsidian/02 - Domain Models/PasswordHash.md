---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, security]
status: implemented
---

# PasswordHash

Value object for BCrypt password hashes.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `value` | String | BCrypt hash (60 chars) |

## Validation

- Must match BCrypt hash pattern (`$2a$10$...`)
- Non-null

## Used By

- [[02 - Domain Models/User\|User]] aggregate root
- [[04 - Ports/outbound/PasswordHasher\|PasswordHasher]] port (creation + verification)

## JPA Mapping

Stored as `VARCHAR(60)` in `users.password_hash` column.
