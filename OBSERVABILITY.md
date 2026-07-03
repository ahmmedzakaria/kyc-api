# Observability

The backend uses Spring Boot Actuator, Micrometer, Prometheus metrics, and OpenTelemetry tracing.

## Backend Endpoints

```text
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

## Local Stack

Start the observability stack:

```bash
OTEL_TRACING_ENABLED=true docker compose --profile observability up -d
```

URLs:

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3002
Tempo:      http://localhost:3200
```

Default Grafana login:

```text
admin / admin
```

## Flow

```text
nexacore-backend
  -> /actuator/prometheus
  -> Prometheus
  -> Grafana

nexacore-backend
  -> OTLP HTTP traces
  -> OpenTelemetry Collector
  -> Tempo
  -> Grafana
```

## Important Environment Variables

```text
OTEL_TRACING_ENABLED=false
OTEL_TRACES_SAMPLER_PROBABILITY=0.10
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://otel-collector:4318/v1/traces
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus,metrics
```

Tracing is disabled by default so normal local/backend runs do not require the OpenTelemetry Collector.
