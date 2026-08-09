# SLIs and SLOs (Reference Targets)

> **Status:** Reference targets for a portfolio/development architecture. Aegis is
> not a commercial service; these are **objectives, not contractual commitments**.
> They define what "healthy" means for each capability and guide alerting.

## Principles

- An **SLI** is a measurement (e.g. request latency).
- An **SLO** is a target over the SLI (e.g. ≥ 99% of requests < 300 ms in 30 days).
- SLOs are **reference**; no external SLA is offered.
- Each SLO maps to a **runbook** (see `docs/runbooks/`) and to dashboards.

## SLIs and SLOs

| Capability | SLI | Target | Measurement window |
|------------|-----|--------|--------------------|
| API availability (BFF/Wallet/Identity) | Request success rate (2xx/4xx/5xx) | ≥ 99% success | rolling 30 days |
| API latency (BFF/Wallet) | p95 request duration | ≤ 300 ms | rolling 30 days |
| Fraud assessment | Time to decision (in-process) | p95 ≤ 200 ms | rolling 7 days |
| Kafka consumer lag | Max lag across consumers | ≤ 5,000 records | rolling 7 days |
| Kafka consumer errors | Records routed to `.dlt` | ≤ 0.1% of messages | rolling 7 days |
| Outbox delivery | Events published within 10 s of commit | ≥ 99.9% | rolling 30 days |
| Outbox backlog | `aegis.outbox.pending_events` | ≤ 10 events | rolling 7 days |
| Ledger integrity | `aegis.wallet.reconciliation_discrepancies` | 0 | every run |
| Database uptime | Health check success rate | ≥ 99% | rolling 30 days |

## Error budget

For availability SLOs an error budget exists:

```
error_budget = 100% - SLO
```

At a 99% availability target over 30 days, the budget is ~7.2 hours of
unavailability. Alerts fire when the budget is trending toward exhaustion.

## Alerting mapping

| SLO breached / trending | Alert | Severity | Runbook |
|-------------------------|-------|----------|---------|
| API availability < target | `APIAvailabilityLow` | P1 | `api-down.md` |
| Consumer lag > threshold | `ConsumerLagHigh` | P2 | `consumer-lag.md` |
| Outbox backlog > 10 | `OutboxBlocked` | P2 | `outbox-blocked.md` |
| Records in `.dlt` increasing | `DltMessages` | P2 | `dlt-inspection.md` |
| Reconciliation discrepancies > 0 | `LedgerDrift` | P1 | `ledger-drift.md` |
| DB connections exhausted | `DbConnectionsExhausted` | P1 | `db-connections.md` |

## Measurement sources

- **HTTP**: Spring Actuator + Prometheus (`http_server_requests_seconds`).
- **Kafka**: Spring Kafka listener metrics (lag, errors).
- **Outbox / reconciliation**: custom Micrometer gauges.
- **DB**: HikariCP metrics + health endpoint.

## See also

- [Runbooks](../runbooks/README.md)
- [Observability Stack](../obsidian/05%20-%20Infrastructure/Observability%20Stack.md)
- [Reconciliation](../architecture/reconciliation.md)
