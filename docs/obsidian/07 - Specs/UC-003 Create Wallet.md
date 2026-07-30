---
type: spec
tags: [spec, wallet]
status: implemented
uc: UC-003
branch: feature/003-create-wallet
---

# UC-003 Create Wallet

**Status**: ✅ Implemented (extended with CRUD operations via Epic #63)

## Overview

Digital wallet creation and lifecycle management. Wallets created with zero balance and configurable per-user limit. Extended with full CRUD (update, deactivate, reactivate).

## Key Files

| Type | Location |
|------|----------|
| Spec | `specs/003-create-wallet/spec.md` |
| Plan | `specs/003-create-wallet/plan.md` |
| Tasks | `specs/003-create-wallet/tasks.md` |
| API Contract | `specs/003-create-wallet/contracts/wallet-api.yaml` |
| Event Schema | `specs/003-create-wallet/contracts/events/wallet-created-event.json` |

## Architecture

- **Service**: [[01 - Services/Wallet Service\|Wallet Service]]
- **Ports**: [[04 - Ports/inbound/CreateWalletUseCase\|CreateWalletUseCase]], [[04 - Ports/inbound/UpdateWalletUseCase\|UpdateWalletUseCase]], [[04 - Ports/inbound/DeactivateWalletUseCase\|DeactivateWalletUseCase]], [[04 - Ports/inbound/ReactivateWalletUseCase\|ReactivateWalletUseCase]]
- **Model**: [[02 - Domain Models/Wallet\|Wallet]]
- **Events**: [[03 - Domain Events/WalletCreated\|WalletCreated]], [[03 - Domain Events/WalletUpdated\|WalletUpdated]], [[03 - Domain Events/WalletDeactivated\|WalletDeactivated]], [[03 - Domain Events/WalletReactivated\|WalletReactivated]]
