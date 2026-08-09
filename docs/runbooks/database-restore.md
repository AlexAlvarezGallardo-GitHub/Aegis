# Runbook: Restore Database

## Symptom

Data loss or corruption affecting one of the service databases (wallet, identity,
fraud, audit, reporting). Operators must restore from backup.

## Severity

P1

## Diagnosis

1. Confirm the scope of the loss (which service DB, which tables).
2. Locate the most recent consistent backup (see
   `docs/operations/backup-recovery.md`).

## Remediation

**Reference procedure (single service DB, e.g. wallet):**

```bash
# 1. Stop the service
docker compose stop wallet-service

# 2. Restore from a logical dump
docker compose exec -T postgres pg_restore \
  --clean --if-exists -U aegis -d wallet \
  < backups/wallet_YYYYMMDD.dump

# 3. Restart and verify
docker compose start wallet-service
```

For schema-only corruption, re-apply Flyway migrations and then restore data.

## Verification

- Service starts and health is UP.
- Reconciliation reports 0 discrepancies (ledger integrity restored).
- Spot-check recent transactions in the ledger.

## Post-incident

- Confirm RPO was met (data loss ≤ target).
- Confirm RTO was met (service restored within target).
- Update `docs/operations/backup-recovery.md` if gaps were found.
