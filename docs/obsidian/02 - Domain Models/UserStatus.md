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
| `LOCKED` | Temporarily locked (5 failed attempts, 15 min) |
| `SUSPENDED` | Admin-suspended |

## State Transitions

```mermaid
stateDiagram-v2
    [*] --> PENDING_VERIFICATION: User.register()
    PENDING_VERIFICATION --> ACTIVE: email verified
    ACTIVE --> LOCKED: 5 failed attempts
    ACTIVE --> SUSPENDED: admin
    LOCKED --> ACTIVE: lockout expired
    SUSPENDED --> [*]
    LOCKED --> [*]
```

## Used By

- [[02 - Domain Models/User\|User]] aggregate root
