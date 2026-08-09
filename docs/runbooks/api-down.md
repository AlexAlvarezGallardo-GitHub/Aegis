# Runbook: API Down

## Symptom

Availability SLO breached (5xx rate rising) or the health endpoint reports DOWN for
a service (BFF, Wallet, Identity).

## Severity

P1

## Diagnosis

```bash
# Health
curl -s localhost:8080/actuator/health | jq
# Recent errors
docker compose logs <service> --tail 200 | grep -i "error\|exception"
```

## Root causes

1. **Dependency down** — the service cannot reach PostgreSQL, Kafka, or Redis.
2. **OOM/restart loop** — the process is crashing (CrashLoopBackOff).
3. **Config error** — bad property or secret on startup.

## Remediation

1. Verify dependencies (`infra/docker-compose.yml` services healthy).
2. Check container restarts: `docker compose ps`.
3. Fix the config / secret and restart the service.
4. If OOM, inspect `-Xmx` and heap usage.

## Verification

- `/actuator/health` returns UP.
- 5xx rate returns to baseline.

## Post-incident

- Record the dependency failure and whether the dependency check/health config
  needs strengthening.
