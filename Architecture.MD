# Architecture

This project is a Maven parent-child microservices system using Kafka choreography-based Saga.

## Modules

1. **common-contracts**: shared REST DTOs and Kafka event contracts.
2. **core-application-service**: user-facing API; fetches products from inventory and submits order requests.
3. **order-service**: Saga coordinator through choreography; reacts to events and drives order state transitions.
4. **inventory-service**: inventory source of truth; reserves/releases stock.
5. **billing-service**: billing transaction service with DB-backed transaction records.

## High-level flow

```text
Client -> Core API -> Kafka(order.commands.requested)
  -> Order Service -> Kafka(inventory.commands.reserve)
    -> Inventory Service -> Kafka(inventory.events.reserved | inventory.events.rejected)
      -> Order Service -> Kafka(billing.commands.charge)
        -> Billing Service -> Kafka(billing.events.charged | billing.events.rejected)
          -> Order Service -> Kafka(order.events.finalized)
            -> Core API (status tracking endpoint)
```

## Observability architecture

```text
Spring Services (Micrometer + OTel)
   -> OTLP traces -> OpenTelemetry Collector -> Jaeger
   -> /actuator/prometheus -> Prometheus -> Grafana dashboards
Kafka broker -> kafka-exporter -> Prometheus -> Grafana
Kafbat UI -> Kafka broker metadata/topic inspection
```

## Saga and compensation

1. `order.commands.requested` starts saga.
2. Inventory reserve failure ends saga as `FAILED`.
3. Billing failure triggers compensation via `inventory.commands.release`.
4. Order completion emits final state to `order.events.finalized`.

## OpenAPI / Swagger

Every service includes springdoc and exposes:

1. API docs: `/api-docs`
2. Swagger UI: `/swagger-ui.html`

## Local runtime ports

1. Core API: `8080`
2. Order Service: `8081`
3. Billing Service: `8082`
4. Inventory Service: `8083`
5. Kafka Broker: `9092`
6. Kafbat UI: `8088`
7. Jaeger UI: `16686`
8. Prometheus: `9090`
9. Grafana: `3000`
