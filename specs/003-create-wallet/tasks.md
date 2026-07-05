# Tasks: UC-003 Create Wallet

## Phase 1: Spec & Infrastructure
- [x] Write spec (spec.md, plan.md, tasks.md)
- [ ] Add wallet PostgreSQL DB to docker-compose.yml
- [ ] Add aegis-wallet-service module to root pom.xml
- [ ] Create aegis-wallet-service pom.xml

## Phase 2: Domain Layer
- [ ] Create WalletId value object
- [ ] Create WalletStatus enum
- [ ] Create LedgerEntryType enum
- [ ] Create LedgerEntry value object
- [ ] Create Wallet aggregate
- [ ] Create WalletCreated domain event
- [ ] Create domain exceptions

## Phase 3: Ports
- [ ] Create CreateWalletUseCase inbound port
- [ ] Create WalletRepository outbound port
- [ ] Create EventPublisher outbound port

## Phase 4: Application Layer
- [ ] Create CreateWalletCommand DTO
- [ ] Create WalletResponse DTO
- [ ] Create WalletMapper
- [ ] Create CreateWalletService

## Phase 5: Infrastructure
- [ ] Create WalletJpaEntity
- [ ] Create LedgerEntryJpaEntity
- [ ] Create JPA repositories
- [ ] Create WalletRepositoryAdapter
- [ ] Create Flyway migration V1
- [ ] Create KafkaEventPublisher with outbox
- [ ] Create OutboxRelayScheduler
- [ ] Create KafkaConfig
- [ ] Create SecurityConfig

## Phase 6: Web Layer
- [ ] Create WalletController (POST, GET list, GET by id)
- [ ] Create WalletExceptionHandler

## Phase 7: Config
- [ ] application.yml (port 8083, DB, Kafka)
- [ ] application-dev.yml

## Phase 8: BFF & Frontend
- [ ] Update BFF proxy config for /api/v1/wallets/*
- [ ] Optional: wallet creation form

## Phase 9: Tests
- [ ] Unit tests for domain model
- [ ] Unit tests for application service
- [ ] Controller test
- [ ] Integration test for endpoint + Kafka
- [ ] Full build
