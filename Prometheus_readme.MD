# Prometheus README

## How Prometheus scrapes Spring Boot actuator metrics

Prometheus uses a **pull model**. It periodically calls each Spring Boot service endpoint:

1. `http://host.docker.internal:8080/actuator/prometheus`
2. `http://host.docker.internal:8081/actuator/prometheus`
3. `http://host.docker.internal:8082/actuator/prometheus`
4. `http://host.docker.internal:8083/actuator/prometheus`

These targets are configured in `kafka-setup/prometheus/prometheus.yml` under `scrape_configs`, with:

1. `metrics_path: /actuator/prometheus`
2. the corresponding `targets` list for each service

## Spring Boot settings required in each service

### Dependencies

1. `org.springframework.boot:spring-boot-starter-actuator`
2. `io.micrometer:micrometer-registry-prometheus`

### Application configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Recommended tag configuration (already used in this project):

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
```

## End-to-end metrics flow

1. Spring Boot app exposes `/actuator/prometheus`
2. Prometheus scrapes metrics at configured intervals
3. Prometheus stores the time series
4. Grafana queries Prometheus to visualize dashboards
