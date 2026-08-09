# Backup, Recovery, RTO and RPO

> **Status:** Reference strategy for the local/DEV reference architecture. No
> production databases exist; this defines the target operational model.

## Objectives

| Metric | Target | Notes |
|--------|--------|-------|
| **RPO** (Recovery Point Objective) | ≤ 5 minutes | Maximum acceptable data loss |
| **RTO** (Recovery Time Objective) | ≤ 30 minutes | Maximum acceptable downtime |

## What is backed up

| Asset | Tool | Frequency |
|-------|------|-----------|
| PostgreSQL databases (wallet, identity, fraud, audit, reporting) | `pg_dump` logical backup | Daily + before migrations |
| Kafka topics | Retained in broker for retention window; event replay for critical topics | Configurable retention |
| Secrets / config | Outside the repo (env vars, secret manager); never in git | On change |
| Docker images | GHCR with immutable tags + SBOM | Per release |

## Backup procedure

Daily logical dumps are stored under `backups/` (gitignored) and can be automated
with a cron job or scheduled workflow:

```bash
mkdir -p backups
docker compose exec -T postgres pg_dump -U aegis -d wallet -Fc > backups/wallet_$(date +%Y%m%d).dump
docker compose exec -T postgres pg_dump -U aegis -d identity -Fc > backups/identity_$(date +%Y%m%d).dump
```

- **Retention:** keep daily backups for 14 days, weekly for 8 weeks, monthly for 12
  months.
- **Encryption:** backups are encrypted at rest (filesystem/volume encryption). For
  offsite copies, use envelope encryption before upload.
- **Integrity:** `pg_restore --list` validates dump integrity after creation.

## Recovery procedure

See the [Restore Database](../runbooks/database-restore.md) runbook for the
step-by-step restoration of a service DB.

## RPO/RTO validation

- **RPO test:** restore a backup and verify no data is missing beyond the last
  backup point (or within the 5-minute window if streaming is used).
- **RTO test:** time a full restore of the wallet DB and confirm it completes under
  30 minutes (includes service restart + reconciliation check).

## Point-in-time recovery (planned)

- Enable PostgreSQL WAL archiving / PITR to reduce RPO to near-zero.
- Add an automated restore test in CI (Testcontainers: restore a dump, run the
  reconciliation job, assert 0 discrepancies).

## See also

- [Runbooks](../runbooks/README.md)
- [Reconciliation](../architecture/reconciliation.md)
- `infra/docker-compose.yml`
