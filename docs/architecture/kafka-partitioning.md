# Kafka Partitioning and Ordering

> **Status:** Documented; producers currently use the event id as the Kafka key
> for most topics. See the gaps below.

## Why partition keys matter

Kafka guarantees **ordering per partition**. Choosing the right partition key
determines whether related events are processed in order by a consumer group.

## Partition key strategy

| Topic | Key (current) | Recommended key | Rationale |
|-------|---------------|-----------------|-----------|
| `wallet.funds.deposited` | `eventId` | `walletId` | Order deposits per wallet; consumers (reporting) upsert by wallet |
| `aegis.wallet.wallet-created` | `eventId` | `walletId` | Aggregate-scoped ordering |
| `aegis.wallet.balance.adjusted` | `eventId` | `walletId` | Balance adjustments must be applied in order per wallet |
| `fraud.assessment.completed` | `eventId` | `transactionId` | Fraud results are per-transaction |
| `payment.transfer.requested` | `eventId` | `sourceWalletId` | Transfer lifecycle per source wallet |
| `aegis.identity.user-registered` | `eventId` | `userId` | Identity events per user |
| `aegis.identity.user-account-locked` | `eventId` | `userId` | Lock state must be ordered per user |

**Current state:** producers publish with `key = eventId` (outbox relay uses
`event.getId().toString()`). This preserves global uniqueness but does **not**
guarantee per-aggregate ordering. Switch the outbox relay to use the aggregate id
as the Kafka key where ordering matters.

## Ordering requirements

| Event pair | Order required? | Why |
|------------|-----------------|-----|
| Deposit → balance adjusted | Per wallet | Reporting projections |
| Deposit → fraud assessment | No | Fraud is per-transaction, independent of balance |
| User registered → user locked | Per user | State transitions on the same aggregate |

## Rebalance behaviour

- Partitions are distributed across consumers in a group; a rebalance may reassign
  partitions, pausing consumption on the affected consumers.
- Consumers are **deduplicated by `eventId`** (`processed_events`), so a rebalance
  that causes redelivery does not double-apply an event.
- Topic with `partitions=1` (`user-account-locked`) serializes all users on one
  partition — acceptable for the low-volume lock topic.

## Outbox key usage

The outbox relay publishes with `key = event.getId().toString()`. To honour the
partitioning strategy, the relay should send with the **aggregate id** stored on
the outbox row (`aggregate_id`). This is a planned change:

```text
producer.send(topic, event.getAggregateId().toString(), event.getPayload())
```

## See also

- [Kafka Topics](../obsidian/05%20-%20Infrastructure/Kafka%20Topics.md)
- [Deposit Flow](sequences/deposit-flow.md)
- [Idempotency](idempotency.md)
