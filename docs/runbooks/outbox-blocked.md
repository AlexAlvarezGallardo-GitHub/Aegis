# Runbook: Outbox Blocked

## Symptom

`aegis.outbox.pending_events` is sustained above 10 across several polling
intervals. Events are not reaching Kafka.

## Severity

P2

## Diagnosis

```bash
# Query pending outbox rows in the wallet service DB
SELECT status, count(*) FROM outbox_events GROUP BY status;
SELECT id, event_type, created_at FROM outbox_events WHERE status = 'PENDING' LIMIT 10;
```

Check the relay scheduler logs:

```bash
docker compose logs wallet-service | grep -i "outbox\|Failed to publish"
```

## Root causes

1. **Kafka unavailable** — broker down or unreachable.
2. **Missing topic mapping** — `aegis.kafka.topics` lacks an entry for an
   `event_type`.
3. **DB issue** — the relay cannot write `PUBLISHED` status.
4. **Batch processing stalled** — the relay hit an exception and `break`s the loop.

## Remediation

1. **Kafka**: `docker compose up -d kafka`; verify with
   `kafka-broker-api-versions.sh --bootstrap-server localhost:9092`.
2. **Topic mapping**: add the missing `eventType -> topic` entry and restart.
3. **DB**: check connectivity; see [db-connections.md](db-connections.md).
4. **Stalled batch**: restart the service; the relay resumes from `PENDING` rows
   (at-least-once; consumers deduplicate by `eventId`).

## Verification

- `aegis.outbox.pending_events` returns to 0.
- No `Failed to publish` messages in the relay logs.

## Post-incident

- Confirm consumers did not double-apply any replayed event (dedup table).
- Document why the relay stalled if it was not Kafka/DB.
