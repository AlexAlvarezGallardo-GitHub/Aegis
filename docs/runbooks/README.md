# Runbooks

Operational playbooks for Aegis. Each runbook describes the symptom, the diagnosis
steps, and the remediation for a known operational issue.

> **Status:** Reference runbooks for the local/DEV reference architecture. No
> on-call rotation exists; these documents encode the expected response for a
> future operator.

| Runbook | Trigger | Severity |
|---------|---------|----------|
| [Consumer Lag High](consumer-lag.md) | Kafka consumer lag > 5,000 | P2 |
| [Outbox Blocked](outbox-blocked.md) | `aegis.outbox.pending_events` > 10 | P2 |
| [Messages in DLT](dlt-inspection.md) | Records landing on `<topic>.dlt` | P2 |
| [Ledger Drift](ledger-drift.md) | Reconciliation discrepancies > 0 | P1 |
| [DB Connections Exhausted](db-connections.md) | HikariCP pool saturated | P1 |
| [API Down](api-down.md) | Availability SLO breach | P1 |
| [Restore Database](database-restore.md) | Data loss / corruption | P1 |

## Runbook template

Every runbook follows this structure:

1. **Symptom** — what the operator observes.
2. **Severity** — P1 (immediate) / P2 (next business day) / P3 (routine).
3. **Diagnosis** — commands and queries to confirm the root cause.
4. **Remediation** — step-by-step fix.
5. **Verification** — how to confirm the issue is resolved.
6. **Post-incident** — notes for a postmortem.
