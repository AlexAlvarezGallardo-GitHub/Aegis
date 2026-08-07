# Runbook: Consumer Lag High

## Symptom

Kafka consumer group lag exceeds 5,000 records for one of the consumer groups
(`audit-group`, `reporting-group`, `fraud-group`).

## Severity

P2

## Diagnosis

```bash
# List consumer groups and their lag
docker compose -f infra/docker-compose.yml exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --all-groups
```

Identify which topic/partition is behind. Check whether the consumer is failing:

```bash
docker compose logs <service> | grep -i "error\|exception\|poison"
```

## Root causes

1. **Poison message** — a record repeatedly fails and the consumer retries until
   the DLT, blocking the partition (default handler retries sequentially).
2. **Downstream slow** — the consumer's DB (PostgreSQL) is slow or saturated.
3. **Consumer down/crashed** — the pod is restarting (CrashLoopBackOff).

## Remediation

1. **Poison message**: inspect the DLT (`<topic>.dlt`). Fix the root cause (schema,
   data), then reprocess. See [dlt-inspection.md](dlt-inspection.md).
2. **Downstream slow**: check DB CPU/connections; see
   [db-connections.md](db-connections.md).
3. **Consumer down**: restart the service; verify it rejoins the group and
   rebalances.

## Verification

- `kafka-consumer-groups.sh --describe --all-groups` shows lag decreasing to ~0.
- No new errors in the consumer logs.

## Post-incident

- Note the root cause (poison message vs. infra) in the postmortem.
- If poison messages recur, review the retry/DLT policy.
