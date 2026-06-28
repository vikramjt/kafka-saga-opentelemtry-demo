# Communication README

## Communication model

The system uses mixed communication:

1. **Synchronous**: core service calls inventory REST API to list products.
2. **Asynchronous**: order, inventory, billing, and core status updates use Kafka events.

## REST communication

1. `GET core-application-service /api/products`
   - Calls `inventory-service /api/inventory/products`
   - Returns product catalog with available quantity and price.
2. `POST core-application-service /api/orders`
   - Accepts customer and product line items.
   - Emits `OrderRequestedEvent` to Kafka.
3. `GET core-application-service /api/orders/{sagaId}`
   - Returns tracked saga state received from final order events.

## Kafka communication topics

1. `order.commands.requested`
   - Producer: Core service
   - Consumer: Order service
   - Purpose: start saga for a new order
2. `inventory.commands.reserve`
   - Producer: Order service
   - Consumer: Inventory service
   - Purpose: reserve requested stock
3. `inventory.events.reserved`
   - Producer: Inventory service
   - Consumer: Order service
   - Purpose: inform successful reservation + total amount
4. `inventory.events.rejected`
   - Producer: Inventory service
   - Consumer: Order service
   - Purpose: inform reservation failure
5. `billing.commands.charge`
   - Producer: Order service
   - Consumer: Billing service
   - Purpose: request payment processing
6. `billing.events.charged`
   - Producer: Billing service
   - Consumer: Order service
   - Purpose: payment success notification
7. `billing.events.rejected`
   - Producer: Billing service
   - Consumer: Order service
   - Purpose: payment failure notification
8. `inventory.commands.release`
   - Producer: Order service
   - Consumer: Inventory service
   - Purpose: compensation for failed payment after stock reservation
9. `order.events.finalized`
   - Producer: Order service
   - Consumer: Core service
   - Purpose: publish final saga status to user-facing layer

## End-to-end sequence (happy path)

1. User submits order to core.
2. Core emits `order.commands.requested`.
3. Order emits `inventory.commands.reserve`.
4. Inventory emits `inventory.events.reserved`.
5. Order emits `billing.commands.charge`.
6. Billing emits `billing.events.charged`.
7. Order emits `order.events.finalized` with `COMPLETED`.

## Compensation sequence (billing failure)

1. Steps 1-5 are same as happy path.
2. Billing emits `billing.events.rejected`.
3. Order emits `inventory.commands.release`.
4. Inventory restores stock.
5. Order emits `order.events.finalized` with `FAILED`.

## Starting Kafka (docker folder)

Kafka setup scripts are in `kafka-setup/`:

1. `./start-kafka.sh`
2. `./stop-kafka.sh`

The scripts run the `docker-compose.yml` with Apache Kafka `4.1.0`.

## Observability communication and tracing

1. Each Spring service exports traces via OTLP HTTP to `http://localhost:4318/v1/traces`.
2. The OpenTelemetry Collector receives traces and forwards them to Jaeger.
3. Kafka headers carry trace context (`traceparent`) between producer and consumer spans.
4. Producer spans are created on send, broker hop is visible via messaging spans, and consumer spans are created on listener processing.

## Runtime UIs

1. Kafbat UI: `http://localhost:8088` (Kafka broker/topic visibility)
2. Jaeger UI: `http://localhost:16686` (trace search and span timeline)
3. Prometheus: `http://localhost:9090` (metrics scrape and query)
4. Grafana: `http://localhost:3000` (dashboard; default `admin/admin`)

## Project-side changes for OpenTelemetry and metrics

1. Added Actuator + Prometheus registry dependencies to all services.
2. Added Micrometer OpenTelemetry bridge and OTLP trace reporter to all services.
3. Enabled Kafka observation:
   - `spring.kafka.template.observation-enabled=true`
   - `spring.kafka.listener.observation-enabled=true`
4. Enabled metrics/tracing endpoints with:
   - `/actuator/prometheus`
   - tracing sampling `1.0` for local debugging
