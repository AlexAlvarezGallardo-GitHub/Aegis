---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, validation]
status: implemented
---

# Email

Value object for validated email addresses.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `value` | String | Normalized email |

## Validation Rules

- Non-null, non-blank
- Matches RFC 5322 pattern
- Normalized to lowercase
- Max 255 characters

## Used By

- [[02 - Domain Models/User\|User]] aggregate root
- `RegisterUserCommand`, `RegisterUserRequest` DTOs

## JPA Mapping

Stored as `VARCHAR(255)` in `users.email` column.
