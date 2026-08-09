# Runbook: Ledger Drift

## Symptom

`aegis.wallet.reconciliation_discrepancies` is greater than 0. The scheduled
reconciliation job reports a wallet whose stored `balance` differs from the sum of
its ledger entries.

## Severity

P1 (financial integrity)

## Diagnosis

Check the reconciliation logs to identify the affected wallet(s):

```bash
docker compose logs wallet-service | grep -i "reconciliation discrepancy"
```

Query the wallet and its ledger:

```sql
SELECT id, balance FROM wallets WHERE id = '<walletId>';
SELECT type, sum(amount) FROM ledger_entries WHERE wallet_id = '<walletId>'
  GROUP BY type;
```

## Root causes

1. A balance update was not accompanied by a ledger entry (or vice versa).
2. A ledger entry was modified or deleted (should be impossible — entries are
   append-only).
3. A reversal did not reduce the balance correctly.

## Remediation

1. **Freeze the wallet** (`PATCH /api/v1/wallets/{id}/status` → `FROZEN`) to stop
   further mutations.
2. Recompute the correct balance from the ledger entries.
3. Apply a **compensating ledger entry** (never edit history) to align the balance.
4. Update the reconciliation baseline; verify the gauge returns to 0.

## Verification

- Reconciliation reports 0 discrepancies.
- A compensating entry exists in the ledger documenting the correction.

## Post-incident

- Full postmortem: how the invariant was broken, why reconciliation caught it, and
  whether an alert fired.
