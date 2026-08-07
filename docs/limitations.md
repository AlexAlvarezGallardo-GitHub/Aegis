# Known Limitations

> **Status:** Explicitly documented. This is a **reference architecture**, not a
> commercial product. Read this before evaluating anything in the repository.

## Non-negotiable facts

| # | Limitation | Detail |
|---|-----------|--------|
| 1 | **No real customer traffic** | All flows are exercised in development; there is no production load. |
| 2 | **Only Local/DEV are functional** | PRE, STAGE and PROD are *prepared structures* demonstrating promotion; they do not operate. |
| 3 | **No regulatory certification** | Not audited, not compliant with PSD2/PCI-DSS/SOX or any financial regulation. |
| 4 | **No banking/KYC integration** | Deposits are simulated; no real bank, card network, or KYC provider is connected. |
| 5 | **No commercial SLA** | SLOs in `docs/observability/slo.md` are reference objectives, not commitments. |
| 6 | **No on-call / operations team** | Runbooks exist but no one is paged. |
| 7 | **No external audit** | Coverage, mutation score and security scans are self-run. |
| 8 | **Simulated capabilities** | Fraud rules, outbox, reconciliation and observability run against local infrastructure. |
| 9 | **AI-assisted development** | Significant code was produced with AI agents under human ownership (see `docs/ai-engineering-governance.md`). |

## Capability-level status

See the canonical matrix in [`docs/project-status.md`](project-status.md) and the
[service catalog](architecture/service-catalog.md) for per-service state.

## What is still planned / incomplete

- Reporting service consumers and projections are partial.
- Balance types (`available`, `pending`, `reserved`) are not implemented
  (see [`wallet-balance.md`](architecture/wallet-balance.md)).
- Chaos-style resilience tests (stopping Kafka/PostgreSQL containers) are planned.
- Point-in-time database recovery (WAL archiving) is planned.
