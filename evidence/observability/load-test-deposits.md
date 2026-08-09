# Load Test Result — Concurrent Deposits (minikube)

- **Date:** 2026-08-07
- **Environment:** minikube (K8s 1.30), wallet-service in `aegis-dev` namespace
- **Tool:** concurrent HTTP jobs (40 parallel deposits)
- **Endpoint:** `POST /api/v1/wallets/{walletId}/deposits`

## Results

| Metric | Value |
|--------|-------|
| Total requests | 40 |
| Success (201) | 40 |
| Errors | 0 |
| Duration | 8.3 s |
| Throughput | 4.8 req/s |
| Latency p50 | 153 ms |
| Latency p95 | 247 ms |
| Latency max | 315 ms |

## Notes

- All deposits succeeded with idempotent references (no 409s — each used a unique
  `LOAD-<i>-<random>` reference).
- Load was generated against the wallet service behind a `kubectl port-forward`
  (8083) on minikube; latency includes the tunnel overhead.
- Traces for this load were exported via OTLP to Tempo (`tempo.monitoring.svc`),
  observable in Grafana → Tempo.

## SLO comparison

The reference SLO (p95 ≤ 400 ms for deposits) was **met**: p95 = 247 ms.
