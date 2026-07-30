# Feature Specification: UC-004 Manage Wallet

**Feature Branch**: `feature/004-manage-wallet`

**Created**: 2026-07-30

**Status**: Draft

---

## Problem

Users need to interact with their wallets after creation — opening a wallet to view details, adjusting balances (deposits/withdrawals), and deactivating wallets when no longer needed. No wallet mutation operations exist beyond creation.

## Solution

Extend the wallet service with **Manage Wallet** use case:
- **Open wallet** — Retrieve wallet details with enriched metadata (premium flag, balance  color indicators)
- **Adjust balance** — Deposit or withdraw funds with full ledger tracking
- **Deactivate wallet** — Set status to FROZEN or CLOSED with business rule enforcement

### Business Rules
1. **No deactivation with non-zero balance** — Wallet cannot be set to FROZEN or CLOSED if its balance is not zero. An exception is thrown.
2. **Balance color coding** — The API returns a `premium` boolean and the `balance` sign. UI interprets: positive = green, negative = red.
3. **Premium tagging** — If the balance exceeds 1000 and the currency is EUR, the wallet is tagged `premium: true`.

---

## Affected Services

| Service | Role |
|---------|------|
| **aegis-wallet-service** | New endpoints + domain logic for wallet management |
| **aegis-bff-service** | Proxy new wallet endpoints |
| **frontend** | Wallet detail view with premium badge and color-coded balance |

---

## Architecture

```
Browser (Angular) ──→ BFF (port 8082) ──→ Wallet Service (port 8083)
                                                │
                                                ├──→ PostgreSQL (aegis_wallet)
                                                ├──→ Kafka topic: wallet.balance.adjusted
                                                └──→ Outbox → Kafka relay
```

### Flow: Adjust Balance
1. Angular → `PATCH /api/bff/wallets/{walletId}/balance` (amount in body)
2. BFF forwards to `PATCH /api/v1/wallets/{walletId}/balance`
3. Wallet Service loads wallet, validates ownership and ACTIVE status
4. Balance adjusted, ledger entry created (DEPOSIT or WITHDRAWAL)
5. `WalletBalanceAdjusted` domain event published via transactional outbox
6. API returns updated wallet detail with premium flag

### Flow: Deactivate Wallet
1. Angular → `PATCH /api/bff/wallets/{walletId}/status` (new status in body)
2. BFF forwards to `PATCH /api/v1/wallets/{walletId}/status`
3. Wallet Service validates: target ≠ ACTIVE, balance == 0
4. Status updated to FROZEN or CLOSED
5. API returns updated wallet detail

---

## API Endpoints

### PATCH /api/v1/wallets/{walletId}/balance
Adjusts the wallet balance (deposit positive amount, withdraw negative amount).

**Request**:
```json
{
  "amount": 500.00,
  "description": "Salary deposit"
}
```

**Response (200)**:
```json
{
  "walletId": "uuid",
  "userId": "uuid",
  "balance": 1500.00,
  "currency": "EUR",
  "status": "ACTIVE",
  "premium": true,
  "createdAt": "2026-07-30T12:00:00Z",
  "updatedAt": "2026-07-30T14:00:00Z"
}
```

### PATCH /api/v1/wallets/{walletId}/status
Updates wallet status (deactivate to FROZEN or CLOSED).

**Request**:
```json
{
  "status": "CLOSED"
}
```

**Response (200)**:
```json
{
  "walletId": "uuid",
  "userId": "uuid",
  "balance": 0.00,
  "currency": "EUR",
  "status": "CLOSED",
  "premium": false,
  "createdAt": "2026-07-30T12:00:00Z",
  "updatedAt": "2026-07-30T14:00:00Z"
}
```

### Error: Cannot deactivate non-zero balance (409)
```json
{
  "code": "WALLET_OPERATION_NOT_ALLOWED",
  "message": "Cannot deactivate wallet with non-zero balance: 500.00",
  "details": null,
  "timestamp": "2026-07-30T14:00:00Z"
}
```

### GET /api/v1/wallets/{walletId} (enhanced)
Now includes `premium` flag in response.

---

## Domain Model Changes

### Wallet — New Methods
- `adjustBalance(BigDecimal amount, String description)` — Creates a ledger entry (DEPOSIT if amount > 0, WITHDRAWAL if amount < 0) and updates balance. Validates wallet is ACTIVE.
- `deactivate(WalletStatus target)` — Sets status to FROZEN or CLOSED. Validates balance is zero.
- `isPremium()` — Returns true if balance > 1000 and currency is EUR.

### WalletStatus
Unchanged: `ACTIVE`, `FROZEN`, `CLOSED`

### LedgerEntryType
Unchanged: `OPENING`, `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_OUT`, `TRANSFER_IN`, `PAYMENT`, `REFUND`

---

## Domain Events

### WalletBalanceAdjusted
New event published when balance changes:
- `walletId: UUID`
- `userId: UUID`
- `previousBalance: BigDecimal`
- `newBalance: BigDecimal`
- `amount: BigDecimal`
- `currency: String`
- `description: String`
- `correlationId: String`

Kafka topic: `aegis.wallet.balance.adjusted`

---

## Sub-Tasks

- [ ] Write spec (spec.md, plan.md, tasks.md)
- [ ] Add domain methods to Wallet.java (adjustBalance, deactivate, isPremium)
- [ ] Create WalletOperationNotAllowedException
- [ ] Create WalletBalanceAdjusted domain event
- [ ] Create UpdateWalletUseCase inbound port
- [ ] Create DTOs (AdjustBalanceCommand, UpdateStatusCommand, WalletDetailResponse)
- [ ] Create UpdateWalletService application service
- [ ] Update WalletMapper (add premium field)
- [ ] Update WalletController (add PATCH endpoints, enhance GET)
- [ ] Update WalletExceptionHandler (add new exception handler)
- [ ] Update OpenAPI contract
- [ ] Update frontend wallet model + component + styles
- [ ] Write tests
