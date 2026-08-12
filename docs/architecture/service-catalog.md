# Aegis Service Catalog

> **This file is the single source of truth** for the number, type, state and capabilities of every Aegis component. Any page, diagram, README, roadmap or metric that contradicts this catalog is wrong and must be updated.

## Canonical definition

Aegis currently contains **7 deployable backend services**, **1 frontend application** and **1 shared Java library**.

This count must remain identical across:

- This catalog (`docs/architecture/service-catalog.md`)
- `README.md`
- `docs/project-status.md`
- The engineering portfolio website
- The roadmap
- The GitOps repository
- The CV

## Components

| Component | Module | Type | State | Persistence | Integration | Port |
|-----------|--------|------|-------|-------------|-------------|------|
| Identity Service | `backend/aegis-identity-service` | Microservice | Implemented · Validated · Deployed in DEV | PostgreSQL (`aegis_identity`) | REST + Kafka (outbox) | 8081 |
| Wallet Service | `backend/aegis-wallet-service` | Microservice | Implemented · Validated · Deployed in DEV | PostgreSQL (`aegis_wallet`) | REST + Kafka (outbox); transfer holds + atomic settlement (UC-005) | 8083 |
| BFF Service | `backend/aegis-bff-service` | Edge service (Backend for Frontend) | Implemented · Validated · Deployed in DEV | Redis (session store) | REST | 8082 |
| Fraud Service | `backend/aegis-fraud-service` | Microservice | Implemented · Validated · Deployed in DEV | PostgreSQL (`aegis_fraud`) | Kafka (consumers + REST) | 8089 |
| Audit Service | `backend/aegis-audit-service` | Microservice | Implemented · Validated · Deployed in DEV | PostgreSQL (`aegis_audit`) | Kafka (consumers) | 8088 |
| Reporting Service | `backend/aegis-reporting-service` | Microservice | Implemented · Partial | PostgreSQL (`aegis_reporting`) | Kafka (consumers) | 8087 |
| Payment Service | `backend/aegis-payment-service` | Microservice | Implemented and Validated and Deployed in DEV | PostgreSQL (`aegis_payment`) | REST + Kafka (outbox) | 8084 |
| Frontend | `frontend/aegis-frontend` | Web application (Angular 22) | Implemented · Validated · Deployed in DEV | — | REST (via BFF) | 4200 |
| Common Library | `backend/aegis-common` | Shared Java library | Implemented | — | Java dependency | — |

## Planned components (not yet implemented)

| Component | Type | Notes |
|-----------|------|-------|
| Notification Service | Microservice | Not implemented — no module, no container |
| API Gateway | Edge service | Not implemented — BFF currently fills the edge role |

## Shared infrastructure

| Component | Version | Purpose |
|-----------|---------|---------|
| PostgreSQL | 16.4-alpine | One database per service (identity, wallet, reporting, audit, fraud, payment) |
| Apache Kafka | 7.5.0 (Confluent) | Event backbone (`aegis.<service>.<event>` topics) + ZooKeeper |
| Redis | 7-alpine | Distributed session store for the BFF |
| Kafka UI | latest | Topic inspection in dev |
| DbGate | 6.2.0 | Database admin UI in dev |

## State definitions

| State | Meaning |
|-------|---------|
| **Implemented** | Working code exists in `main` and is part of the docker-compose stack |
| **Validated** | Automated tests cover the component (unit/integration/contract) and CI runs them |
| **Deployed in DEV** | Runs in the local docker-compose DEV environment with real infrastructure |
| **Partial** | Core structure works but not all intended capabilities are operational |
| **Prepared** | Structure created, not yet operational (used by PRE/STAGE/PROD environments) |
| **Planned** | Not implemented yet |

## How to verify this catalog

- Backend modules: `backend/*/pom.xml` — every `aegis-*` module with a `Dockerfile` is a deployable service.
- Deployment stack: `infra/docker-compose.yml` — every service container in this file must appear in the table above.
- Tests: each module's `src/test/**/*Test.java` and `*IT.java` files.

## Update rules

1. When a component changes state, update this file **and** `README.md` **and** `docs/project-status.md` in the same change.
2. Never introduce a new number of services anywhere without updating this file first.
3. Every pull request that adds or removes a deployable module must update this catalog.
