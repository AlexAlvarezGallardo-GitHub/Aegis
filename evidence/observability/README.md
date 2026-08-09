# Observability Evidence

> **Status:** Captured against the running stack (minikube + Aegis-GitOps LGTM).
> These are **real traces and load results**, not simulated data.

> **Visual evidence:** open [`evidence-dashboard.png`](evidence-dashboard.png) or
> [`evidence-visual.html`](evidence-visual.html) to see a rendered dashboard of the
> captured traces and load metrics.

> **Grafana screenshots (real Tempo UI):**
> - [`grafana-tempo-traces.png`](grafana-tempo-traces.png) — Explore → Tempo showing
>   real traces of `aegis-wallet-service` (outbox relay, reconciliation, health).
> - [`grafana-trace-detail.png`](grafana-trace-detail.png) — trace waterfall of the
>   outbox relay span (`relay-pending-events`, 1.15 ms).

## Environment

- **Cluster:** minikube (Kubernetes 1.30, Docker driver)
- **Stack:** deployed via Aegis-GitOps kustomize (Prometheus, Grafana, Tempo,
  Alertmanager, Loki in `monitoring`/`logging` namespaces)
- **Services:** Aegis services in `aegis-dev` namespace, each exporting OTLP to
  `tempo.monitoring.svc:4318/v1/traces`

## Captured traces

| File | Trace | What it shows |
|------|-------|---------------|
| `grafana-tempo-traces.png` | Grafana Tempo Explore | Real trace list of `aegis-wallet-service` in the Grafana UI |
| `grafana-trace-detail.png` | Grafana trace waterfall | Outbox relay span detail (1.15 ms) in the Grafana UI |
| `evidence-dashboard.png` | Visual dashboard | Screenshot of the evidence dashboard (traces + load results) |
| `evidence-visual.html` | Visual dashboard | HTML source of the evidence dashboard (open in a browser) |
| `trace-outbox-relay.json` | Outbox relay span | `relayPendingEvents` producing to Kafka; `outcome: SUCCESS`; OTel SDK 1.37.0 |
| `trace-http-health.json` | HTTP health request | Full Spring Security filterchain spans (12 filters, `authorize request` granted) |
| `trace-http-prometheus.json` | Prometheus scrape | HTTP scrape span of `/actuator/prometheus` |

Trace IDs (example):
- Outbox relay: `6453d64c1255b774b2248ba0bd9a8eb8`
- HTTP health: `17ca3c037147e63d39f2af979b0446cb`
- Prometheus scrape: `42805887cbf99f42ce68df4aa5d8e0fc`

## Generated flows

1. Created a wallet (`POST /api/v1/wallets` → `019fdbef-220a-...`).
2. Deposited EUR 150 (`POST .../deposits`, reference `OBS-DEMO-...`) → event
   `FUNDS_DEPOSITED` published to `wallet.funds.deposited` (verified in Kafka).
3. Deposited EUR 25 (`OBS-TRACE-...`) with `X-Correlation-Id: trace-evidence-001`.
4. Load test: 40 concurrent deposits (see `load-test-deposits.md`).

## Load test result

`load-test-deposits.md` — 40/40 success, 0 errors, throughput 4.8 req/s,
latency p50 153 ms / p95 247 ms / max 315 ms (meets the deposit p95 ≤ 400 ms SLO).

## Kafka trace propagation

The W3C `traceparent` propagation across Kafka is verified by the integration test
`KafkaTracePropagationIT` (Wallet service, Testcontainers), which asserts a
published message carries the `traceparent` header in `00-<trace>-<span>-<flags>`
format. The deployed DEV image predates the `observation-enabled` change
(PR #180); the propagation itself is covered by CI.

## How to reproduce

```bash
minikube start
kubectl apply -k infrastructure/monitoring          # from Aegis-GitOps
kubectl port-forward -n monitoring svc/tempo 4318:4318
kubectl port-forward -n aegis-dev svc/wallet-dev 8083:8083

# create + deposit
curl -X POST localhost:8083/api/v1/wallets -H "X-User-Id: <uuid>" \
  -H "Content-Type: application/json" -d '{"currency":"EUR"}'
curl -X POST localhost:8083/api/v1/wallets/<walletId>/deposits \
  -H "X-User-Id: <uuid>" -H "Content-Type: application/json" \
  -d '{"amount":100,"currency":"EUR","source":"BANK_TRANSFER","reference":"REP-1"}'

# query Tempo for wallet traces
kubectl exec -n monitoring deploy/tempo -- wget -qO- \
  'http://localhost:3200/api/search?tags=service.name=aegis-wallet-service&limit=5'
```

## See also

- [Distributed tracing (OTLP)](../../docs/observability/tracing.md)
- [SLIs and SLOs](../../docs/observability/slo.md)
- [Load testing](../../load/README.md)
