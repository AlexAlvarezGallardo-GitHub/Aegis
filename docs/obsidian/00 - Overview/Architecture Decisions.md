---
type: adr-index
tags: [architecture, decisions, adr]
status: implemented
---

# Architecture Decisions

## Principles (from Constitution)

| # | Principle | Description |
|---|-----------|-------------|
| I | Hexagonal Architecture | Strict port/adapter separation |
| II | Domain Ownership | Each service owns its DB |
| III | Event-Driven Communication | Kafka via transactional outbox |
| IV | Security-First | BCrypt, JWT, constant-time |
| V | Test-Driven Quality | 80%+ coverage, 100% domain |

## Key Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Identity | UUIDv7 for User IDs | Time-ordered, DB-friendly |
| Auth | JWT access + refresh tokens | Stateless, scalable |
| Password | BCrypt hashing | Industry standard |
| Wallet | BigDecimal for balance | No floating-point errors |
| Wallet | 5 wallets max per user | Configurable limit |
| Events | Transactional outbox | Exactly-once semantics |
| BFF | Redis-backed HttpSession | Distributed session store |
| Frontend | Angular Material | Enterprise design system |
| Frontend | Gold (#D4AF37) theme | Brand identity |
| Dev | Docker Compose + DevTools | Hot-reload development |
