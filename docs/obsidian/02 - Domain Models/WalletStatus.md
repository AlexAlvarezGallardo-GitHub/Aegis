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

```text
                 ┌─────────┐
          ┌──────│  ACTIVE  │──────┐
          │      └─────────┘      │
          ▼                       ▼
    ┌─────────┐            ┌──────────┐
    │  FROZEN │            │  CLOSED  │
    └─────────┘            └──────────┘
          ▲                       ▲
          └───────────────────────┘
         (reactivate from both)
```

## Used By

- [[02 - Domain Models/Wallet\|Wallet]] aggregate root
- Displayed via `StatusChipComponent` in frontend
