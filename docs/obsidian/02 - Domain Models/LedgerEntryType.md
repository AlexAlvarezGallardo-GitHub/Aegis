---
type: value-object
service: aegis-wallet-service
layer: domain
tags: [ddd, enum, ledger]
status: implemented
---

# LedgerEntryType

Enum classifying immutable ledger entry types.

## Values

| Value | Effect | Description |
|-------|--------|-------------|
| `OPENING` | +0 | Wallet creation entry |
| `DEPOSIT` | +balance | Funds added |
| `WITHDRAWAL` | -balance | Funds deducted |
| `TRANSFER_OUT` | -balance | Funds sent to another wallet |
| `TRANSFER_IN` | +balance | Funds received from another wallet |
| `PAYMENT` | -balance | Funds paid out |
| `REFUND` | +balance | Funds refunded |
| `REVERSAL` | -balance | Reverses a DEPOSIT (references original entry) |

## State Transitions

Ledger entry types represent immutable transaction classifications. Each entry is created once with a fixed type. The "transitions" below represent the logical flow of funds through the wallet lifecycle:

```mermaid
stateDiagram-v2
    [*] --> OPENING: Wallet.create()
    [*] --> DEPOSIT: deposit funds
    [*] --> WITHDRAWAL: withdraw funds
    [*] --> TRANSFER_OUT: send transfer
    [*] --> TRANSFER_IN: receive transfer
    [*] --> PAYMENT: pay
    [*] --> REFUND: refund
    DEPOSIT --> REVERSAL: reverse deposit (references original)
    OPENING --> [*]
    DEPOSIT --> [*]
    WITHDRAWAL --> [*]
    TRANSFER_OUT --> [*]
    TRANSFER_IN --> [*]
    PAYMENT --> [*]
    REFUND --> [*]
    REVERSAL --> [*]
```

## Used By

- [[02 - Domain Models/LedgerEntry\|LedgerEntry]] value object
