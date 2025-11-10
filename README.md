# Distributed tracing demo services

This repository contains three Spring Boot 3.2.0 services built with Java 21 that demonstrate REST, gRPC, WebSocket, and NATS (message queue) interactions.

## Modules

- **common-proto** – Shared gRPC contracts compiled from protobuf definitions.
- **inventory-service** – Exposes REST and gRPC endpoints for inventory status and listens to NATS messages.
- **operation-service** – Invokes the inventory service via REST and gRPC and publishes updates to NATS.
- **gateway-service** – Provides REST, gRPC, and WebSocket interfaces for clients.

## Building

```bash
mvn clean package
```

## Running the services

Each service can be launched separately. They expect supporting infrastructure (such as NATS) to already be running.

```bash
# terminal 1
mvn -pl inventory-service spring-boot:run

# terminal 2
mvn -pl operation-service spring-boot:run

# terminal 3
mvn -pl gateway-service spring-boot:run
```

Environment variables can be used to override defaults:

- `NATS_URL` – NATS server URL (default `nats://localhost:4222`).
- `INVENTORY_HTTP_BASE_URL` – Inventory REST URL for the operation service.
- `INVENTORY_GRPC_TARGET` – Inventory gRPC target for the operation service.

## Sample interactions

```bash
# Trigger the operation workflow (REST -> REST + gRPC + NATS)
curl http://localhost:8080/operations/execute/demo-item

# Call inventory REST endpoint directly
curl http://localhost:8081/inventory/status/demo-item

# Call gateway REST endpoint
curl http://localhost:8082/api/ping?name=Tempo
```

For gRPC, use any compatible client targeting the ports defined in each service (`9091`, `9092`). WebSocket clients can connect to `ws://localhost:8082/ws/ping` and send text payloads.
