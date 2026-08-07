# Runbook: Database Connections Exhausted

## Symptom

HikariCP pool is saturated; services return timeouts/`SQLTransientConnectionException`.
`hikaricp_connections_pending` and `hikaricp_connections_active` are at max.

## Severity

P1

## Diagnosis

```bash
# Check HikariCP metrics via actuator
curl localhost:8080/actuator/prometheus | grep hikaricp
# Inspect DB connections
docker compose exec postgres psql -U aegis -c "SELECT state, count(*) FROM pg_stat_activity GROUP BY state;"
```

## Root causes

1. **Leaked connections** — a transaction never closes (usually a missing
   `@Transactional` boundary or a long-running query).
2. **Slow queries** — long-running queries occupy pool slots.
3. **Pool misconfiguration** — `maximum-pool-size` too low for the concurrency.

## Remediation

1. Identify long-running queries: `SELECT pid, now()-query_start, query FROM pg_stat_activity WHERE state='active' ORDER BY query_start;`
2. Kill runaway queries: `SELECT pg_terminate_backend(<pid>);`
3. Review the service code for leaked transactions.
4. If necessary, raise `spring.datasource.hikari.maximum-pool-size`.

## Verification

- `hikaricp_connections_pending` returns to 0; health endpoint is UP.

## Post-incident

- Add query-slow-log alerts; review pool sizing.
