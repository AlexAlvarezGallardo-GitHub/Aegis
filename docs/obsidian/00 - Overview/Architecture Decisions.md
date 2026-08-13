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
| Fraud | DB-backed rules + strategy evaluators | Configurable without redeploy (ADR-001) |
| Fraud | Risk score thresholds (30/70) | APPROVE/REVIEW/REJECT |
| Kafka | Configuration-driven topics | Rename/wire without code (ADR-002) |
| BFF | Redis-backed HttpSession | Distributed session store |
| Frontend | Angular Material | Enterprise design system |
| Frontend | Gold (#D4AF37) theme | Brand identity |
| Dev | Docker Compose + DevTools | Hot-reload development |

## ADR Index

All ADRs live in `docs/adr/`. ADR-007 is intentionally absent (number skipped). All are **Accepted**.

| ADR | Title | Status |
|-----|-------|--------|
| ADR-001 | Fraud Rules Configuration Strategy and Extensibility | Accepted |
| ADR-002 | Kafka Topic Configuration Strategy | Accepted |
| ADR-003 | Defer Kubernetes and Helm to a Later Phase | Accepted |
| ADR-004 | Ledger Design — Single-Entry Per-Aggregate Ledger | Accepted |
| ADR-005 | Kafka as the Event Backbone | Accepted |
| ADR-006 | Transactional Outbox Pattern | Accepted |
| ADR-008 | Idempotency Strategy | Accepted |
| ADR-009 | Event Versioning and Standard Envelope | Accepted |
| ADR-010 | Retry Policy and Dead Letter Topics | Accepted |
| ADR-011 | Backend-for-Frontend (BFF) for the Web Client | Accepted |
| ADR-012 | OpenTelemetry for Observability | Accepted |
| ADR-013 | Secrets Management — Externalized via Environment | Accepted |
| ADR-014 | Orchestrated Saga with Fund Reservation for Cross-Wallet Transfers | Accepted |
| ADR-015 | Merchant Payments Reuse the Transfer Saga Mechanics | Accepted |
| ADR-016 | Refunds are New Ledger Credits, Not Settlements | Accepted |
