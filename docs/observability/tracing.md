# Distributed Tracing (OpenTelemetry / OTLP)

> **Status:** Implemented. Every backend service is instrumented with Micrometer
> Tracing (OpenTelemetry bridge) and exports traces via OTLP/HTTP. Trace context
> propagates across HTTP and Kafka boundaries.

## Overview

```
Frontend ──HTTP──> BFF ──HTTP──> Wallet ──Kafka(outbox)──> Consumers (Audit/Reporting/Fraud)
   └─────────────── traceparent (W3C) ───────────────────┘
                              │
                    OTLP/HTTP :4318 ──> Tempo ──> Grafana
```

A single trace ID spans the whole request: the BFF request, the Wallet deposit
transaction, the outbox relay, and each consumer that processes the resulting
event. The trace ID is also injected into the SLF4J MDC (`traceId`/`spanId`) so
logs from any service can be correlated to the same trace.

## Instrumentation

| Piece | Library | Config |
|-------|---------|--------|
| Tracing bridge | `micrometer-tracing-bridge-otel` | `management.tracing.sampling.probability: 1.0` |
| Exporter | `opentelemetry-exporter-otlp` | `management.otlp.tracing.endpoint` (OTLP/HTTP) |
| HTTP propagation | Spring MVC auto | W3C `traceparent` request/response headers |
| Kafka propagation | Spring Kafka observation | `spring.kafka.*.observation-enabled: true` |
| Log correlation | `logstash-logback-encoder` | `traceId`/`spanId` in MDC |

## Configuration (per service)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: ${OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}
      transport: http
spring:
  kafka:
    producer:
      observation-enabled: true   # producers (identity, wallet, fraud, reporting)
    consumer:
      observation-enabled: true   # consumers (audit, reporting, fraud)
    listener:
      observation-enabled: true   # @KafkaListener containers
```

- `OTLP_TRACING_ENDPOINT` overrides the collector address (defaults to
  `localhost:4318`, matching Tempo's OTLP/HTTP port in the compose/K8s stack).
- Sampling is `1.0` for development. In production this should be reduced (e.g.
  `0.1` + tail-based sampling) to control volume.

## Kafka trace propagation

Spring Kafka's observation support (enabled via `observation-enabled: true`) uses
the Micrometer `KafkaTracingObservationHandler` to attach the W3C `traceparent`
header to published records and to continue the active trace on the consumer side.

- **Producer**: when a message is sent inside an active span, the header
  `traceparent: 00-<traceId>-<spanId>-<flags>` is added to the record.
- **Consumer**: the listener observes the incoming record; if `traceparent` is
  present, the trace is continued; otherwise a new span is started.

**Verification** — `KafkaTracePropagationIT` (Wallet service) publishes inside an
active span and asserts the consumed record carries a `traceparent` header in the
W3C format (55 chars).

## Verifying traces end-to-end

1. Start the stack (Tempo + Grafana + services).
2. Trigger a flow (e.g. a deposit via the BFF/Wallet).
3. In Grafana → **Explore** → data source **Tempo** → Search by
   `resource.service.name` or the span attribute `http.route = /api/v1/wallets/{id}/deposits`.
4. The trace should show the BFF → Wallet → Kafka producer → consumer spans linked
   by the same trace ID.

> Tempo index note: traces become searchable after the ingester block flushes
> (configured to `5m`). If a trace is not found immediately, wait and re-search.

## Known limits

- `spring.task.scheduling.observation-enabled: false` disables the outbox
  scheduler's per-poll span to avoid noise (see Observability Stack doc).
- The `traceparent` header is propagated, but trace context is not yet injected
  into the Kafka record **value** (only headers) — enough for distributed tracing.

## See also

- [Observability Stack](../obsidian/05%20-%20Infrastructure/Observability%20Stack.md)
- [SLIs and SLOs](slo.md)
- Dashboards: `infra/observability/dashboards/`
