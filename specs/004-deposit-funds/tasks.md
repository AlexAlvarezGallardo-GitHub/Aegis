# Tasks: UC-004 Deposit Funds

## Phase 1: Spec
- [x] Write spec (spec.md, plan.md, tasks.md)
- [ ] Create GitHub issue (#7)

## Phase 2: Domain Layer
- [x] Create FundsDeposited domain event
- [x] Create DuplicateDepositException
- [x] Add depositFunds method to Wallet.java

## Phase 3: Ports
- [x] Create DepositFundsUseCase inbound port
- [x] Add FundsDeposited to EventPublisher

## Phase 4: Application Layer
- [x] Create DepositFundsCommand DTO
- [x] Create DepositReceipt DTO
- [x] Create DepositFundsService

## Phase 5: Web Layer
- [x] Add POST /api/v1/wallets/{walletId}/deposits endpoint
- [x] Add DuplicateDepositException handler

## Phase 6: BFF
- [x] Add proxy endpoint for deposits

## Phase 7: Tests
- [ ] Unit tests for Wallet.depositFunds
- [ ] Unit tests for DepositFundsService
- [ ] Controller test for deposit endpoint
