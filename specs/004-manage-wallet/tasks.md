# Tasks: UC-004 Manage Wallet

## Phase 1: Spec
- [ ] Write spec (spec.md, plan.md, tasks.md)
- [x] Create GitHub issue (#75)

## Phase 2: Domain Layer
- [ ] Add adjustBalance method to Wallet.java
- [ ] Add deactivate method to Wallet.java
- [ ] Add isPremium method to Wallet.java
- [ ] Create WalletOperationNotAllowedException
- [ ] Create WalletBalanceAdjusted domain event

## Phase 3: Ports
- [ ] Create UpdateWalletUseCase inbound port

## Phase 4: Application Layer
- [ ] Create AdjustBalanceCommand DTO
- [ ] Create UpdateStatusCommand DTO
- [ ] Create WalletDetailResponse DTO (with premium flag)
- [ ] Update WalletMapper to include premium
- [ ] Create UpdateWalletService

## Phase 5: Web Layer
- [ ] Add PATCH /api/v1/wallets/{walletId}/balance endpoint
- [ ] Add PATCH /api/v1/wallets/{walletId}/status endpoint
- [ ] Enhance GET /api/v1/wallets/{walletId} to include premium
- [ ] Add WalletOperationNotAllowedException handler

## Phase 6: OpenAPI
- [ ] Update wallet-api.yaml with new endpoints

## Phase 7: Frontend
- [ ] Update wallet.model.ts with premium field
- [ ] Add getBalanceColor method to wallet.component.ts
- [ ] Add premium badge to wallet.component.html
- [ ] Add color-coded balance styles to wallet.component.scss

## Phase 8: Tests
- [ ] Unit tests for new Wallet domain methods
- [ ] Unit tests for UpdateWalletService
- [ ] Controller tests for new endpoints
