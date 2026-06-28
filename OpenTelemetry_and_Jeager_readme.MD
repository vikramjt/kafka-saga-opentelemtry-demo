# OpenTelemetry README

## What is configured in this project

OpenTelemetry is configured for **distributed tracing** across all services:

1. `core-application-service`
2. `order-service`
3. `inventory-service`
4. `billing-service`

Each service uses Spring Boot OpenTelemetry starter and exports traces with OTLP.

## Service-side configuration

In each service `application.yml`:

1. Tracing sampling is enabled at 100%:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
```

2. OTLP trace export endpoint is configured:

```yaml
management:
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
```

3. Kafka observation is enabled for producer and listener:

```yaml
spring:
  kafka:
    template:
      observation-enabled: true
    listener:
      observation-enabled: true
```

4. Log correlation is enabled:

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

Additionally, custom Kafka beans explicitly enable observation (`kafkaTemplate.setObservationEnabled(true)` and listener container observation enabled) so `traceparent` propagation works with custom Kafka config.

## Collector-side configuration

From `kafka-setup/otel/otel-collector-config.yml`:

1. Receivers:
   - OTLP gRPC on `4317`
   - OTLP HTTP on `4318`
2. Processor:
   - `batch`
3. Exporters:
   - OTLP exporter to Jaeger (`jaeger:4317`)
   - debug exporter (collector logs)
4. Pipeline:
   - `traces` only (no metrics/logs pipeline configured)

## What telemetry is produced and exported

### 1. Traces (Produced and Exported)

The system produces and exports trace spans for:

1. Incoming HTTP server requests (Spring Web)
2. Outgoing HTTP client calls (where applicable)
3. Kafka producer send operations
4. Kafka consumer listener processing

These traces are exported:

1. Service -> OTLP (`4318`) -> OTEL Collector
2. OTEL Collector -> Jaeger
3. Viewed in Jaeger UI (`http://localhost:16686`)

Kafka messages include trace context headers (for example `traceparent`) so one transaction can be correlated across services.

### 2. Metrics

Metrics are **not exported via OTLP** in this project.

Reason: OTLP metrics push was disabled to avoid 404 errors and because metrics are already handled by Prometheus pull.

Current metrics path:

1. Services expose `/actuator/prometheus`
2. Prometheus scrapes those endpoints
3. Grafana reads from Prometheus

### 3. Logs

Logs are not exported through OTEL collector as log data pipeline.
But logs include `traceId` and `spanId` correlation values to connect logs with traces in Jaeger.

## How to verify quickly

1. Trigger a business flow (place an order).
2. In Kafbat, inspect Kafka message headers and verify `traceparent`.
3. In Jaeger, search by service and time range; open a trace to view cross-service spans.
4. In service logs, check the `traceId` and `spanId` values in log lines.

## How OpenTelemetry and Jaeger work together in this project

1. Each Spring service creates spans (HTTP server/client and Kafka producer/consumer operations).
2. Each service exports spans through OTLP HTTP to `http://localhost:4318/v1/traces`.
3. OTEL Collector receives spans on OTLP receiver ports (`4317` gRPC / `4318` HTTP).
4. Collector batches spans and forwards them to Jaeger using OTLP (`jaeger:4317`).
5. Jaeger stores and indexes traces, and exposes them in the UI at `http://localhost:16686`.

For service-to-service communication over Kafka, trace context is propagated in Kafka headers (`traceparent`), so spans from core -> order -> inventory -> billing stay under the same distributed trace in Jaeger.

## Why `jaeger:4317` works for OTLP export

Jaeger is not "part of OpenTelemetry", but Jaeger can expose an **OTLP ingest endpoint**.

In this project:

1. Docker Compose starts Jaeger with:

```yaml
jaeger:
  image: jaegertracing/all-in-one:1.61.0
  environment:
    COLLECTOR_OTLP_ENABLED: "true"
```

2. `COLLECTOR_OTLP_ENABLED=true` enables Jaeger Collector OTLP receiver.
3. That OTLP receiver listens on port `4317` (gRPC) inside the docker network.
4. Therefore OTEL Collector can export traces to Jaeger using:

```yaml
exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
```

So `4317` here is being used by Jaeger's OTLP collector endpoint, not by OTEL Collector itself.

## Required settings chain (docker-compose -> OTEL collector -> services)

To make Jaeger receive traces on `4317`, these settings must all exist:

1. **Jaeger container**
   - `COLLECTOR_OTLP_ENABLED: "true"` in `kafka-setup/docker-compose.yml`

2. **OTEL Collector exporter**
   - `exporters.otlp/jaeger.endpoint: jaeger:4317` in `kafka-setup/otel/otel-collector-config.yml`
   - traces pipeline must include that exporter

3. **Service export endpoint**
   - each service exports OTLP traces to OTEL Collector (`http://localhost:4318/v1/traces`) via:
   - `management.opentelemetry.tracing.export.otlp.endpoint`

End-to-end:

1. Service creates spans
2. Service -> OTEL Collector (`4318`)
3. OTEL Collector -> Jaeger (`jaeger:4317`)
4. Jaeger UI shows traces
