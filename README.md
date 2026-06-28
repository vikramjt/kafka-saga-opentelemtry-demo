# Retail Saga Platform (Kafka + Spring Boot Microservices)

This project is a Maven multi-module microservices platform that implements an **event-driven Saga pattern** for ordering workflows using **Apache Kafka**.

It contains:

1. **core-application-service**: User-facing API to view products and submit orders.
2. **order-service**: Saga workflow service that drives orchestration by reacting to events.
3. **inventory-service**: Product inventory and stock reservation/compensation.
4. **billing-service**: Billing transaction processing.
5. **common-contracts**: Shared DTOs and Kafka event contracts.

---

## 1. Architecture Overview

High-level business flow:

```text
Client -> Core API -> Kafka(order.commands.requested)
  -> Order Service -> Kafka(inventory.commands.reserve)
    -> Inventory Service -> Kafka(inventory.events.reserved | inventory.events.rejected)
      -> Order Service -> Kafka(billing.commands.charge)
        -> Billing Service -> Kafka(billing.events.charged | billing.events.rejected)
          -> Order Service -> Kafka(order.events.finalized)
            -> Core API (status endpoint)
```

### Saga and compensation

1. Order starts with `order.commands.requested`.
2. Inventory reserves stock.
3. Billing charges payment.
4. If billing fails, compensation is triggered using `inventory.commands.release`.
5. Final status is published to `order.events.finalized`.

---

## 2. Technologies Used (and why)

1. **Java 21**: LTS runtime and language baseline.
2. **Spring Boot 4.1.0**: Service framework for REST, config, lifecycle, and dependency management.
3. **Spring Kafka**: Producer/consumer integration and listener abstractions.
4. **Spring Data JPA + H2**: Local persistence for order/billing/inventory states.
5. **Springdoc OpenAPI**: Swagger/OpenAPI docs per service.
6. **Micrometer + Actuator**: Metrics and health endpoints.
7. **OpenTelemetry (OTLP)**: Distributed tracing export from all services.
8. **Kafka (KRaft mode)**: Event broker for async choreography.
9. **Kafbat UI**: Kafka topic/group visibility and inspection.
10. **Prometheus + Grafana**: Metrics storage/query + dashboards.
11. **Jaeger + OTEL Collector**: Trace collection pipeline and trace visualization.
12. **Docker Compose**: One-command local infrastructure startup.

---

## 3. Project Structure

```text
.
├── pom.xml                       # Parent Maven POM
├── common-contracts/             # Shared API + Kafka event contracts
├── core-application-service/     # Port 8080
├── order-service/                # Port 8081
├── billing-service/              # Port 8082
├── inventory-service/            # Port 8083
└── kafka-setup/                  # Kafka + observability docker stack
```

---

## 4. Local Ports

1. Core API: `http://localhost:8080`
2. Order Service: `http://localhost:8081`
3. Billing Service: `http://localhost:8082`
4. Inventory Service: `http://localhost:8083`
5. Kafka Broker: `localhost:9092`
6. Kafbat UI: `http://localhost:8088`
7. Jaeger UI: `http://localhost:16686`
8. Prometheus: `http://localhost:9090`
9. Grafana: `http://localhost:3000` (`admin/admin`)

---

## 5. Prerequisites

1. Java 21
2. Docker + Docker Compose
3. Maven wrapper (already included: `./mvnw`)

---

## 6. How to Start the Application

### Step A: Start Kafka + observability stack

```bash
cd kafka-setup
chmod +x start-kafka.sh stop-kafka.sh
./start-kafka.sh
```

### Step B: Build modules

```bash
cd ..
./mvnw clean install
```

### Step C: Start all services (in separate terminals)

Terminal 1:

```bash
./mvnw -pl inventory-service spring-boot:run
```

Terminal 2:

```bash
./mvnw -pl billing-service spring-boot:run
```

Terminal 3:

```bash
./mvnw -pl order-service spring-boot:run
```

Terminal 4:

```bash
./mvnw -pl core-application-service spring-boot:run
```

---

## 7. API Docs / Swagger

Each service exposes:

1. OpenAPI JSON: `/api-docs`
2. Swagger UI: `/swagger-ui.html`

Examples:

1. `http://localhost:8080/swagger-ui.html`
2. `http://localhost:8081/swagger-ui.html`
3. `http://localhost:8082/swagger-ui.html`
4. `http://localhost:8083/swagger-ui.html`

---

## 8. How to Run Flows

### 8.1 Check products

```bash
curl -s http://localhost:8080/api/products | jq
```

### 8.2 Happy path order

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-1001",
    "items": [
      {"productId": "P-100", "quantity": 1},
      {"productId": "P-200", "quantity": 2}
    ]
  }' | jq
```

Response includes `sagaId`.

Check status:

```bash
curl -s http://localhost:8080/api/orders/<SAGA_ID> | jq
```

Expected final status: `COMPLETED`.

### 8.3 Compensation flow (force billing failure)

Billing service rejects when `customerId` contains `FAIL_BILLING`.

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "FAIL_BILLING_TEST",
    "items": [
      {"productId": "P-100", "quantity": 1}
    ]
  }' | jq
```

Check status:

```bash
curl -s http://localhost:8080/api/orders/<SAGA_ID> | jq
```

Expected final status: `FAILED`, and inventory release compensation is triggered.

---

## 9. Observability: How to Use

### 9.1 Kafka UI (Kafbat)

Open `http://localhost:8088` to inspect:

1. Topics
2. Consumer groups
3. Message metadata
4. Partition/offset state

### 9.2 Tracing (Jaeger)

Open `http://localhost:16686`:

1. Select service (`core-application-service`, `order-service`, etc.)
2. Search recent traces
3. View publish/consume spans across Kafka hops

### 9.3 Metrics (Prometheus + Grafana)

1. Prometheus: `http://localhost:9090`
2. Grafana: `http://localhost:3000` (admin/admin)
3. Pre-provisioned dashboard: **Retail Kafka Observability**

Dashboard includes:

1. Kafka brokers/partitions/consumer lag
2. Service CPU and heap memory
3. HTTP request rate
4. Kafka topic offset growth

---

## 10. Operational Endpoints

Each service exposes:

1. `GET /actuator/health`
2. `GET /actuator/metrics`
3. `GET /actuator/prometheus`

---

## 11. Topic Map

1. `order.commands.requested` (Core -> Order)
2. `inventory.commands.reserve` (Order -> Inventory)
3. `inventory.events.reserved` (Inventory -> Order)
4. `inventory.events.rejected` (Inventory -> Order)
5. `billing.commands.charge` (Order -> Billing)
6. `billing.events.charged` (Billing -> Order)
7. `billing.events.rejected` (Billing -> Order)
8. `inventory.commands.release` (Order -> Inventory compensation)
9. `order.events.finalized` (Order -> Core)

---

## 12. Shutdown

1. Stop each Spring Boot service (`Ctrl+C` in each terminal).
2. Stop infrastructure:

```bash
cd kafka-setup
./stop-kafka.sh
```

---

## 13. Additional Documentation

1. `Architecture.MD` – architecture and ports
2. `Communication_readme.MD` – detailed service/topic interactions
3. `Kafka_readme.MD` – Kafka scaling and production tuning guidance
