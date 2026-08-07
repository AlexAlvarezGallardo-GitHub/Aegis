# Load Testing (k6)

> **Status:** Scripts added. Execution requires the local/DEV stack running
> (`infra/docker-compose.yml`).

## Tooling

[k6](https://k6.io) — chosen for its scriptability, low overhead, and CI-friendly
CLI. Scripts live in `load/k6/`.

## Scenarios

| Script | Scenario | Key assertion |
|--------|----------|---------------|
| `login.js` | BFF login flow | 200 + session cookie; p95 < 300 ms |
| `wallets.js` | Create + list wallets | 201 / 200; p95 < 300 ms |
| `deposits.js` | Concurrent deposits (UC-004) | 201 + new balance; p95 < 400 ms |
| `idempotency.js` | Reuse the same deposit reference | first 201, repeats 409 (no double-apply) |

## Running

```bash
# login (50 VUs over 2 min)
k6 run --vus 50 --duration 2m load/k6/login.js

# wallets
k6 run load/k6/wallets.js

# concurrent deposits
k6 run load/k6/deposits.js

# idempotency
k6 run load/k6/idempotency.js
```

`BASE_URL` defaults to the BFF (`http://localhost:8082`); override with
`-e BASE_URL=...`.

## Environment

Run against the local stack:

```bash
docker compose -f infra/docker-compose.yml up -d
```

The BFF listens on `8082`. Create a user and a wallet before exercising the
deposit scenarios, or rely on the per-VU wallet creation inside the scripts.

## Reporting

Recommended report fields for a load run (include in any evidence/PR):

- hardware/OS and stack version (JDK, docker images)
- duration, VUs, arrival rate
- throughput (req/s), p50/p95/p99 latencies
- error rate and any 4xx/5xx
- bottleneck notes (CPU, DB pool, Kafka lag)

Example dashboard entry:

```text
Scenario: deposits.js, 20 VUs, 2m, 8-core/16GB, JDK 21
Requests: 2400, Throughput: 20 req/s, p95: 210 ms, Errors: 0%
Bottleneck: none at this load; DB pool 10/50 active
```

## CI integration (planned)

A `load-test.yml` workflow can run k6 against the DEV stack on schedule (e.g.
nightly) and publish a summary artifact. Scripts are already CI-ready (no local
state).

## See also

- [SLIs and SLOs](../docs/observability/slo.md)
- [Deposit Flow](../docs/architecture/sequences/deposit-flow.md)
- [Resilience tests](../docs/architecture/resilience-testing.md)
