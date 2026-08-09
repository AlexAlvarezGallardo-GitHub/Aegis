# Runbook: Messages in Dead Letter Topic (DLT)

## Symptom

Records are landing on `<topic>.dlt` (e.g. `wallet.funds.deposited.dlt`). The
`retry`/`dlt` message count is increasing.

## Severity

P2

## Diagnosis

Consume the DLT and inspect the original payload and headers:

```bash
docker compose -f infra/docker-compose.yml exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic wallet.funds.deposited.dlt \
  --from-beginning --property print.headers=true
```

## Root causes

1. **Deserialization failure** — payload does not match the event schema (schema
   drift, malformed JSON).
2. **Business logic exception** — the consumer throws an unrecoverable error after
   retries.
3. **Schema mismatch between producer and consumer** — e.g. event version changed.

## Remediation

1. Inspect the message headers for the original topic/partition/offset and any
   trace/correlation IDs.
2. Fix the root cause:
   - **Schema drift**: align the producer and consumer event records; version the
     event (see `docs/architecture/event-versioning.md`).
   - **Business error**: fix the consumer logic or the data.
3. **Reprocess**: republish the records back onto the source topic (or the `.retry`
   topic) with the corrected schema.

## Verification

- DLT message count stops growing.
- Reprocessed records are applied by the consumer (dedup prevents re-applying the
  original failures incorrectly — check `processed_events`).

## Post-incident

- If the same message recurs, escalate the schema contract review.
