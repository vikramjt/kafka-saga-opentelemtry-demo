# Kafka README

This document explains Kafka setup and production scaling considerations for this architecture.

## Versions used

1. Apache Kafka: **4.1.0**
2. Java target in project: **21**
3. Spring Boot parent: **4.1.0**

## Local single-node setup

For local development, `kafka-setup/docker-compose.yml` runs one KRaft broker/controller.

Commands:

1. `cd kafka-setup`
2. `chmod +x start-kafka.sh stop-kafka.sh`
3. `./start-kafka.sh`
4. `./stop-kafka.sh`

## Included local observability stack

The docker stack now includes:

1. **Kafbat UI** (`:8088`) for broker/topic/consumer-group browsing.
2. **OpenTelemetry Collector** for OTLP ingest and forwarding.
3. **Jaeger** (`:16686`) for distributed trace visualization.
4. **Prometheus** (`:9090`) for Kafka and service metrics scraping.
5. **Grafana** (`:3000`) for pre-provisioned dashboards.
6. **kafka-exporter** (`:9308`) for Kafka cluster and lag metrics.

### How traces move end-to-end

1. Producer service creates span for Kafka publish.
2. Trace context is injected into Kafka record headers.
3. Consumer service extracts context and links processing span.
4. Services push spans to OTEL Collector (`/v1/traces`).
5. Collector forwards to Jaeger for query and timeline view.

### How metrics move end-to-end

1. Spring services expose `/actuator/prometheus`.
2. kafka-exporter exposes Kafka broker/topic/consumer metrics.
3. Prometheus scrapes all targets.
4. Grafana dashboard reads from Prometheus datasource.

## Why partitions matter

A Kafka topic is split into partitions. Partitions control:

1. Maximum parallel consumer throughput.
2. Ordering scope (strict ordering only inside one partition).
3. Data distribution across brokers.

For saga workflows:

1. Use `sagaId` as Kafka message key.
2. All events for one saga then stay ordered in the same partition.
3. Different saga IDs are processed in parallel across partitions.

## Scaling strategy by volume

When volume increases, scale in this order:

1. **Increase partitions** on hot topics first.
2. **Increase consumer instances** in each consumer group to match partition count.
3. **Add brokers** when single-broker CPU/network/disk reaches limits.
4. **Raise replication factor** for durability and broker fault tolerance.

## Partitions and consumer concurrency

Rules:

1. One partition is consumed by only one consumer instance in the same group at a time.
2. Maximum active consumers per group = partition count.
3. Extra consumers remain idle.

Example:

1. Topic `billing.commands.charge` has 24 partitions.
2. Run 12 billing service instances -> all active.
3. Run 30 billing service instances -> only 24 active for that topic.

## Replication and ISR

For production durability:

1. Use `replication.factor=3` for critical topics.
2. Set `min.insync.replicas=2`.
3. Use producer `acks=all`.

Result:

1. One broker can fail without data loss for committed writes.
2. Producer write succeeds only when enough replicas acknowledge.

Trade-off:

1. Higher durability adds write latency.
2. Lower ISR/acks improves latency but increases data-loss risk.

## Broker count planning

Practical recommendation:

1. Start with **3 brokers** for production baseline.
2. Move to **5 or more brokers** as partition count, retention, and throughput increase.

Heuristics:

1. Keep partition leaders spread across brokers.
2. Avoid placing all hot partitions on one node.
3. Track CPU, network I/O, disk IOPS, and request latency.

## Topic-level settings to review

For high-volume environments:

1. `num.partitions`
2. `replication.factor`
3. `min.insync.replicas`
4. `retention.ms`
5. `retention.bytes`
6. `cleanup.policy` (`delete` vs `compact`)
7. `segment.bytes`
8. `compression.type` (`zstd` or `lz4` typically)

## Producer-side tuning

Important producer settings:

1. `acks=all`
2. `enable.idempotence=true`
3. `retries` high enough for transient failures
4. `linger.ms` small positive value to improve batching
5. `batch.size` tuned to payload size and throughput
6. `compression.type=zstd` or `lz4`

These settings improve reliability and throughput for event-driven Saga systems.

## Consumer-side tuning

Important consumer settings:

1. `max.poll.records`
2. `fetch.min.bytes`
3. `fetch.max.wait.ms`
4. `max.partition.fetch.bytes`
5. `session.timeout.ms`
6. `heartbeat.interval.ms`
7. cooperative rebalancing strategy

Also:

1. Use distinct consumer groups per service role.
2. Keep processing idempotent to tolerate retries/rebalances.

## Scaling from 1 broker to N brokers

When scaling from local to production:

1. Deploy 3+ Kafka brokers (KRaft controller quorum sized appropriately).
2. Increase topic replication from 1 to 3.
3. Increase partitions for high-traffic topics.
4. Reassign partitions to balance leaders/followers.
5. Increase service instances to consume in parallel.
6. Validate end-to-end lag and broker resource metrics.

## Recommended production defaults

1. Brokers: `3`
2. Replication factor: `3`
3. Min ISR: `2`
4. Acks: `all`
5. Partition count per topic: start `12` (increase by load tests)
6. Key by `sagaId` to preserve per-order event ordering

## Notes specific to this project

Current service topic beans are configured with:

1. `partitions=6`
2. `replicas=1`

This is suitable for local development. For production, externalize these values and align with multi-broker cluster capacity and SLA.
