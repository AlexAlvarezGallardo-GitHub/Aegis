---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, auth]
status: implemented
---

# Credentials

Value object holding validated authentication input.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `email` | [[02 - Domain Models/Email\|Email]] | Validated email |
| `password` | String | Raw password (must not be blank) |

## Validation Rules

- Password must be non-null and non-blank
- Email is a validated [[02 - Domain Models/Email|Email]] value object

## Used By

- `AuthenticateUserCommand` (email/password/correlationId) in the application layer
- `AuthenticateUserService`
