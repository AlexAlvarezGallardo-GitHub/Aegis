# Technical Debt Register

> **Status:** Active register. Items are tracked with impact, priority and planned
> action. Transparent debt is a feature of a mature portfolio.

| # | Debt | Impact | Priority | Action |
|---|------|--------|----------|--------|
| 1 | Dead letter topics have no automated reprocessing tool | Poison messages require manual replay | High | Add a DLT replay script/CLI |
| 2 | Reporting service consumers/projections incomplete | Reporting capability is partial | High | Finish consumers + projections |
| 3 | Kafka partition keys are `eventId`, not the aggregate id | No per-aggregate ordering | Medium | Use aggregate id as the key in the outbox relay |
| 4 | `balance` is a single field (no available/pending/reserved) | No holds/reservations support | Medium | Add balance types when payment authorizations arrive |
| 5 | Environments PRE/STAGE/PROD are prepared but not operating | Promotion can't be validated end-to-end | Medium | Activate a staging environment |
| 6 | Local secrets in `infra/.env` (dev-only) | Risk of drift into production config | Medium | Move production secrets to a secret manager |
| 7 | No point-in-time recovery (WAL archiving) | RPO limited to daily dump | Medium | Enable PITR + restore test |
| 8 | Load/chaos tests not scheduled in CI | No continuous performance signal | Low | Add a scheduled k6 + chaos workflow |
| 9 | Frontend lacks e2e coverage for all happy paths | Regression risk in the UI | Medium | Expand Playwright suites |
| 10 | Trace context not in the Kafka record value (headers only) | Slightly harder to correlate at consumers | Low | Inject trace context into envelope when needed |
