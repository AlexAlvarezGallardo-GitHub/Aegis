# Implementation Plan: UC-004 Manage Wallet

**Branch**: `feature/004-manage-wallet` | **Date**: 2026-07-30

## Implementation Order

```
1. Domain Layer
   ├── WalletOperationNotAllowedException.java
   ├── WalletBalanceAdjusted.java (domain event)
   └── Wallet.java (adjustBalance, deactivate, isPremium)
2. Ports Layer
   └── UpdateWalletUseCase.java (inbound port)
3. Application Layer
   ├── AdjustBalanceCommand.java (DTO)
   ├── UpdateStatusCommand.java (DTO)
   ├── WalletDetailResponse.java (DTO with premium)
   ├── WalletMapper.java (update with premium)
   └── UpdateWalletService.java (application service)
4. Web Layer
   ├── WalletController.java (add PATCH endpoints, enhance GET)
   └── WalletExceptionHandler.java (new exception handler)
5. OpenAPI Contract
   └── wallet-api.yaml (updated)
6. Frontend
   ├── wallet.model.ts (premium field)
   ├── wallet.component.ts (color/premium methods)
   ├── wallet.component.html (premium badge, color balance)
   └── wallet.component.scss (green/red/premium styles)
7. BFF proxy config (if needed)
8. Tests
```

## Key Decisions

| Decision | Choice |
|----------|--------|
| Balance mutation | Absolute amount with positive = deposit, negative = withdrawal |
| Balance validation | Wallet must be ACTIVE to adjust balance |
| Deactivation rule | Balance must be exactly zero (ZERO.compareTo == 0) |
| Premium threshold | Balance > 1000 AND currency == "EUR" |
| Premium evaluation | Computed each time; stored in response, not persisted |
| Ledger entry amounts | Always stored as absolute values, type determines sign |
| Event for balance change | New `WalletBalanceAdjusted` event on Kafka topic `aegis.wallet.balance.adjusted` |
