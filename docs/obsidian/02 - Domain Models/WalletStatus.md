---
type: value-object
service: aegis-wallet-service
layer: domain
tags: [ddd, enum, wallet]
status: implemented
---

# WalletStatus

Enum representing wallet lifecycle states.

## Values

| Value | Color | Description |
|-------|-------|-------------|
| `ACTIVE` | 🟢 Green | Fully operational |
| `FROZEN` | 🔵 Blue | Temporarily frozen |
| `CLOSED` | ⚫ Grey | Deactivated |

## State Transitions

```mermaid
stateDiagram-v2
    [*] --> ACTIVE
    ACTIVE --> FROZEN: freeze (balance = 0)
    ACTIVE --> CLOSED: close (balance = 0)
    FROZEN --> ACTIVE: reactivate
    CLOSED --> ACTIVE: reactivate
    CLOSED --> [*]
```

## Used By

- [[02 - Domain Models/Wallet\|Wallet]] aggregate root
- Displayed via `StatusChipComponent` in frontend
