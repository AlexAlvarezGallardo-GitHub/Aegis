# Implementation Plan: UC-004 Deposit Funds

**Branch**: `feature/004-deposit-funds` | **Date**: 2026-07-30

## Implementation Order

```
1. Domain Layer
   ├── FundsDeposited.java (domain event)
   ├── DuplicateDepositException.java
   └── Wallet.java (depositFunds method)
2. Ports Layer
   ├── DepositFundsUseCase.java (inbound port)
   └── EventPublisher.java (add FundsDeposited overload)
3. Application Layer
   ├── DepositFundsCommand.java (DTO)
   ├── DepositReceipt.java (DTO)
   └── DepositFundsService.java (application service)
4. Infrastructure
   └── KafkaEventPublisher.java (handle FundsDeposited)
5. Web Layer
   ├── WalletController.java (add POST /{id}/deposits)
   └── WalletExceptionHandler.java (add handler)
6. BFF
   └── BffWalletController.java (proxy endpoint)
7. Tests
   ├── Unit tests
   └── Integration tests
```

## Key Decisions

| Decision | Choice |
|----------|--------|
| Idempotency | Check by external reference in ledger entries |
| Amount validation | Must be positive (> 0) |
| Deposit endpoint | POST (not PATCH) for semantic correctness |
| Deposit event | Separate `FundsDeposited` (not reusing `WalletBalanceAdjusted`) |
| Source tracking | Required field in request |
