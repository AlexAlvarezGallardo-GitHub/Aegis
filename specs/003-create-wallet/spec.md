# Feature Specification: UC-003 Create Wallet

**Feature Branch**: `feature/003-create-wallet`

**Created**: 2026-07-05

**Status**: Draft

---

## Problem

Users need a digital wallet to store and manage funds within the Aegis platform. No wallet entity exists yet — this is the foundational financial primitive.

## Solution

A new **Wallet Service** (`aegis-wallet-service`) with hexagonal architecture that handles wallet creation for authenticated users. Wallets are created with zero initial balance and a configurable per-user limit.

---

## Affected Services

| Service | Role |
|---------|------|
| **aegis-wallet-service** | New — manages wallet lifecycle, ledger entries |
| **aegis-bff-service** | Extends proxy config to route `/api/v1/wallets` |
| **frontend** | Wallet creation form (optional in this phase) |
| **PostgreSQL** | New database `aegis_wallet` |

---

## Architecture

```
Browser (Angular) ──→ BFF (port 8082) ──→ Wallet Service (port 8083)
                                                │
                                                ├──→ PostgreSQL (aegis_wallet)
                                                ├──→ Kafka topic: wallet.created
                                                └──→ Outbox → Kafka relay
```

### Flow: Create Wallet
1. Angular → `POST /api/bff/wallets` (currency, via BFF)
2. BFF attaches JWT from session, forwards to `POST /api/v1/wallets`
3. Wallet Service validates user eligibility (per-user wallet limit)
4. Wallet entity created with zero balance, ACTIVE status
5. Ledger initialized with an opening entry
6. `WalletCreated` domain event published via transactional outbox
7. API returns wallet details (id, balance, currency, status)

---

## API Endpoints

### POST /api/v1/wallets
Creates a new wallet for the authenticated user.

**Request**:
```json
{ "currency": "EUR" }
```

**Response (201)**:
```json
{
  "walletId": "uuid",
  "userId": "uuid",
  "balance": 0.00,
  "currency": "EUR",
  "status": "ACTIVE",
  "createdAt": "2026-07-05T12:00:00Z"
}
```

### GET /api/v1/wallets
Lists wallets for the authenticated user.

**Response (200)**:
```json
{
  "wallets": [
    {
      "walletId": "uuid",
      "balance": 0.00,
      "currency": "EUR",
      "status": "ACTIVE",
      "createdAt": "2026-07-05T12:00:00Z"
    }
  ]
}
```

### GET /api/v1/wallets/{id}
Gets a single wallet by ID (must belong to authenticated user).

---

## Domain Model

### Wallet
- `walletId: WalletId` (UUIDv7)
- `userId: UUID` (owner)
- `balance: BigDecimal` (immutable, updated via ledger)
- `currency: Currency` (ISO 4217)
- `status: WalletStatus` (ACTIVE, FROZEN, CLOSED)
- `createdAt: Instant`
- `updatedAt: Instant`
- `version: Long` (optimistic locking)

### LedgerEntry
- `id: UUID` (UUIDv7)
- `walletId: UUID`
- `type: LedgerEntryType` (OPENING, DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, PAYMENT, REFUND)
- `amount: BigDecimal`
- `currency: Currency`
- `reference: String`
- `timestamp: Instant`

### WalletStatus
- `ACTIVE` — normal operations
- `FROZEN` — no transactions allowed
- `CLOSED` — permanently closed

---

## Dependencies (POM)

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-kafka`
- `postgresql`
- `flyway-core` + `flyway-database-postgresql`
- `aegis-common` (shared exceptions, UUID generator)

---

## Sub-Tasks

- [x] Write spec (spec.md, plan.md, tasks.md)
- [ ] Create `aegis-wallet-service` Maven module + pom.xml
- [ ] Create `WalletServiceApplication.java` main class
- [ ] Create domain model (WalletId, Wallet, LedgerEntry, WalletStatus, LedgerEntryType)
- [ ] Create `WalletCreated` domain event
- [ ] Create `CreateWalletUseCase` inbound port
- [ ] Create `WalletRepository` outbound port
- [ ] Create `CreateWalletService` application service
- [ ] Create DTOs and mapper
- [ ] Create JPA entities + adapters
- [ ] Create Kafka event publisher with outbox
- [ ] Create `WalletController` (POST /api/v1/wallets, GET /api/v1/wallets, GET /api/v1/wallets/{id})
- [ ] Create `SecurityConfig` + exception handler
- [ ] Add wallet DB to docker-compose.yml
- [ ] Add module to root pom.xml
- [ ] Update BFF proxy config for wallet routes
- [ ] Write tests
