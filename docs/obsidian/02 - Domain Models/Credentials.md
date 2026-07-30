---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, auth]
status: implemented
---

# Credentials

Value object holding raw authentication input.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `email` | String | Raw email input |
| `password` | String | Raw password input |

## Used By

- [[04 - Ports/inbound/AuthenticateUserUseCase\|AuthenticateUserUseCase]]
- `AuthenticateUserService`
