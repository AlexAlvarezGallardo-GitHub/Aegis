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

| Value | Description |
|-------|-------------|
| `ACTIVE` | Fully operational |
| `FROZEN` | Temporarily frozen |
| `CLOSED` | Closed (deactivated) |

## State Transitions

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: Wallet.create()
    ACTIVE --> FROZEN: deactivate(FROZEN), balance = 0
    ACTIVE --> CLOSED: deactivate(CLOSED), balance = 0
    FROZEN --> CLOSED: deactivate(CLOSED), balance = 0
    CLOSED --> [*]
```

## Used By

- [[02 - Domain Models/Wallet\|Wallet]] aggregate root
- Displayed via `StatusChipComponent` in frontend
