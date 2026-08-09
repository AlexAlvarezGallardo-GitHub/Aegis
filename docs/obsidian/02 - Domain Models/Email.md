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
| `value` | String | Normalized (trimmed, lowercase) email |

## Validation Rules

- Non-null, non-blank
- Max 255 characters
- Matches the domain pattern `^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`
- Normalized to trimmed lowercase; throws `InvalidEmailException`

## Factory Methods

- `Email.of(value)` → validated instance or `InvalidEmailException`

## Used By

- [[02 - Domain Models/User|User]] aggregate root
- [[02 - Domain Models/Credentials|Credentials]] value object
- `RegisterUserCommand`, `AuthenticateUserCommand`, `RegisterUserRequest` DTOs

## JPA Mapping

Stored as `VARCHAR(255)` in `users.email` column.
