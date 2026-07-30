---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, enum, identity]
status: implemented
---

# UserStatus

Enum representing account lifecycle states.

## Values

| Value | Description |
|-------|-------------|
| `PENDING_VERIFICATION` | Awaiting email verification |
| `ACTIVE` | Fully registered and active |
| `LOCKED` | Temporarily locked (failed attempts) |
| `SUSPENDED` | Admin-suspended |
| `CLOSED` | Account closed |

## State Transitions

```text
PENDING_VERIFICATION → ACTIVE
ACTIVE → LOCKED (5 failed attempts)
ACTIVE → SUSPENDED (admin)
ACTIVE → CLOSED (user request)
LOCKED → ACTIVE (timeout or admin unlock)
SUSPENDED → ACTIVE (admin)
```

## Used By

- [[02 - Domain Models/User\|User]] aggregate root
