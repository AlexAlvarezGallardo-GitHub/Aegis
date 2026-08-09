# ADR-012: OpenTelemetry for Observability

## Status

Accepted

## Date

2026-08-07

## Context

The platform needs distributed traces spanning multiple services (BFF → Wallet →
Kafka → consumers), correlated with metrics and logs. Without a standard
instrumentation API, each service would vendor its own tracing SDK and traces
would not stitch together.

## Decision

**Use OpenTelemetry via Micrometer Tracing** as the observability instrumentation.

- **Bridge**: `micrometer-tracing-bridge-otel` (Spring Boot-managed).
- **Exporter**: `opentelemetry-exporter-otlp` sending OTLP/HTTP to
  `OTLP_TRACING_ENDPOINT` (default `http://localhost:4318/v1/traces`, Tempo).
- **HTTP**: W3C `traceparent` propagates across service calls automatically.
- **Kafka**: `spring.kafka.*.observation-enabled: true` propagates the trace
  through records (see ADR-013 / tracing doc).
- **Logs**: `traceId`/`spanId` injected into SLF4J MDC for correlation.

## Alternatives Considered

### Alternative 1: Vendor SDK directly (e.g. Jaeger client)
- **Pros**: direct control.
- **Cons**: vendor lock-in; not integrated with Micrometer/Spring Boot.

### Alternative 2: No distributed tracing
- **Pros**: zero deps.
- **Cons**: cannot debug cross-service flows.

**Why not chosen**: Micrometer Tracing + OTLP is the Spring-native, vendor-neutral
path; it integrates with metrics and logs.

## Consequences

### Positive
- End-to-end traces across HTTP and Kafka.
- Correlated logs and metrics.

### Negative
- Sampling must be tuned (1.0 in dev; lower in prod) to control volume.

### Risks
- **Risk**: Tempo unavailable at startup — **Mitigation**: the exporter is
  non-blocking; traces are buffered/dropped gracefully.

## Related Decisions

- ADR-005 (Kafka backbone) — trace propagation across events.

## References

- `docs/observability/tracing.md`
- `docs/obsidian/05 - Infrastructure/Observability Stack.md`
