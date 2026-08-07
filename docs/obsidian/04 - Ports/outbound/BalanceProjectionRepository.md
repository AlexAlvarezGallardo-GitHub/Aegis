---
type: port
service: aegis-reporting-service
layer: domain
tags: [port, outbound, repository, reporting, read-model]
status: implemented
port-type: outbound
---

# BalanceProjectionRepository

Outbound port for persisting and querying [[02 - Domain Models/BalanceProjection|BalanceProjection]] read-models.

## Methods

| Method | Description |
|--------|-------------|
| `save(projection)` → `BalanceProjection` | Persist a balance projection (insert or update) |
| `findByWalletId(walletId)` → `Optional<BalanceProjection>` | Lookup the projection for a wallet |

## Implementation

- **Adapter**: in `infrastructure/persistence/`
- **Consumer**: upserts the projection when [[03 - Domain Events/FundsDeposited|FundsDeposited]] is consumed from `wallet.funds.deposited`

## Used By

- `FundsDepositedConsumer` in [[01 - Services/Reporting Service|Reporting Service]]
