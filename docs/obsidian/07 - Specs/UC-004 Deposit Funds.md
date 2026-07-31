---
type: spec
tags: [spec, uc-004, wallet, deposit]
status: draft
feature-branch: feature/004-deposit-funds
---

# UC-004: Deposit Funds

**Feature Branch**: `feature/004-deposit-funds`

## Overview

Dedicated endpoint for depositing funds into wallets with source tracking, idempotency by external reference, and separate event emission for downstream consumers (reporting, audit).

## Scope

- **Wallet Service**: `POST /api/v1/wallets/{id}/deposits`
- **BFF Service**: Proxies deposit endpoint
- **Reporting Service**: Consumes `FundsDeposited` to update balance projections
- **Audit Service**: Consumes `FundsDeposited` to persist audit trail

## Business Rules

1. Amount must be positive (> 0)
2. Wallet must be ACTIVE
3. External reference must be unique (idempotency)

## Events

| Event | Topic | Producer | Consumers |
|-------|-------|----------|-----------|
| [[03 - Domain Events/FundsDeposited\|FundsDeposited]] | `wallet.funds.deposited` | Wallet | Reporting, Audit |

## API

`POST /api/v1/wallets/{walletId}/deposits` — Deposits funds, returns receipt.

## Implementation

See `specs/004-deposit-funds/` for full spec, plan, and tasks.
