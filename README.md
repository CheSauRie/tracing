# Distributed tracing demo services

This repository contains Spring Boot 3.2.0 services built with Java 21 that demonstrate REST, gRPC, WebSocket, and NATS (message queue) interactions. OpenTelemetry emission is handled through Spring Boot's Micrometer tracing bridge so the applications stay "zero-code" with respect to OpenTelemetry APIs while still producing OTLP traces suitable for Tempo or any OTLP compatible collector. Each service is an independent Maven project so they can be built and deployed separately and stitched together with an Istio gateway.

## Projects

- **common-proto** – Shared gRPC contracts compiled from protobuf definitions.
- **inventory-service** – Exposes REST (`/inventory/status/{itemId}`) and gRPC (`InventoryService/CheckInventory`) endpoints for inventory status and publishes inventory events to NATS.
- **operation-service** – Invokes the inventory service via REST and gRPC, publishes updates to NATS, and exposes REST (`/operations/execute/{itemId}`) and WebSocket (`/ws/operations`) interfaces for clients.
- **istio/** – Example Istio `Gateway`, `VirtualService`, and `DestinationRule` resources that surface the two services to clients over HTTP, gRPC, and WebSocket without adding another Spring gateway.

## Prerequisites

- Java 21
- Maven 3.9+
- A running NATS server (`nats://localhost:4222` by default)
- An OTLP-compatible collector endpoint (Tempo, etc.) reachable at `http://localhost:4317` by default
- An Istio-enabled Kubernetes cluster if you plan to apply the manifests in `istio/`

## Build

Install the shared protobuf types and then package the services individually:

```bash
mvn -f common-proto/pom.xml clean install
mvn -f inventory-service/pom.xml clean package
mvn -f operation-service/pom.xml clean package
```

> **Note:** The `common-proto` artifact must be installed or published before building the services that depend on it.

## Run locally

In separate terminals:

```bash
mvn -f common-proto/pom.xml clean install
mvn -f inventory-service/pom.xml spring-boot:run
mvn -f operation-service/pom.xml spring-boot:run
```

Environment variables can be used to override defaults:

- `OTEL_EXPORTER_OTLP_ENDPOINT` – OTLP trace exporter endpoint consumed by the Micrometer OTLP bridge (default `http://localhost:4317`).
- `NATS_URL` – NATS server URL (default `nats://localhost:4222`).
- `INVENTORY_HTTP_BASE_URL` – Inventory REST URL for the operation service (default `http://localhost:8081`).
- `INVENTORY_GRPC_TARGET` – Inventory gRPC target for the operation service (default `static://localhost:9091`).

## Sample interactions

```bash
# Trigger the operation workflow (REST -> REST + gRPC + NATS -> WebSocket broadcast)
curl http://localhost:8080/operations/execute/demo-item

# Call the inventory REST endpoint directly
curl http://localhost:8081/inventory/status/demo-item

# Connect a WebSocket client (e.g. using websocat)
websocat ws://localhost:8080/ws/operations
```

For gRPC, use any compatible client targeting the Istio route (`/com.example.proto.InventoryService/`) or directly call the inventory service on port `9091` when running locally.

## Deploy with Istio

Apply the manifests after deploying the two services into the same namespace:

```bash
kubectl apply -f istio/gateway.yaml
kubectl apply -f istio/destination-rules.yaml
```

The Istio gateway exposes:

- HTTP routes for `/operations` and `/inventory`
- A WebSocket-capable route for `/ws/operations`
- A gRPC route for the `InventoryService` API

Traces from both services are exported via OTLP through Micrometer's OpenTelemetry bridge. For deeper instrumentation (JDBC, Reactor internals, etc.) you can optionally attach the OpenTelemetry Java agent when starting each service:

```bash
java -javaagent:/path/to/opentelemetry-javaagent.jar -jar target/inventory-service-0.0.1-SNAPSHOT.jar
```

The Java agent layers on additional instrumentation without requiring any OpenTelemetry-specific code in the services.
