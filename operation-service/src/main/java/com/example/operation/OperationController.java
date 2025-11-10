package com.example.operation;

import com.example.proto.InventoryReply;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.nats.client.Connection;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/operations")
public class OperationController {

    private static final Logger log = LoggerFactory.getLogger(OperationController.class);

    private final WebClient webClient;
    private final InventoryGrpcClient inventoryGrpcClient;
    private final Connection connection;
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;

    public OperationController(WebClient webClient,
                               InventoryGrpcClient inventoryGrpcClient,
                               Connection connection,
                               ObservationRegistry observationRegistry,
                               Tracer tracer) {
        this.webClient = webClient;
        this.inventoryGrpcClient = inventoryGrpcClient;
        this.connection = connection;
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
    }

    @GetMapping("/execute/{itemId}")
    public Mono<ResponseEntity<Map<String, Object>>> execute(@PathVariable String itemId) {
        return Observation.createNotStarted("operation.execute", observationRegistry)
                .observe(() -> webClient.get()
                        .uri("/inventory/status/{itemId}", itemId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .flatMap(restResponse -> Mono.fromCallable(() -> {
                            InventoryReply reply = inventoryGrpcClient.stub().checkInventory(
                                    com.example.proto.InventoryRequest.newBuilder().setItemId(itemId).build());

                            publishNatsEvent(itemId, reply.getStatus());

                            return ResponseEntity.ok(Map.of(
                                    "itemId", itemId,
                                    "restStatus", restResponse.get("status"),
                                    "grpcStatus", reply.getStatus(),
                                    "timestamp", Instant.now().toString()
                            ));
                        }).subscribeOn(Schedulers.boundedElastic())));
    }

    private void publishNatsEvent(String itemId, String status) {
        Span span = tracer.spanBuilder("operation-service.nats.publish")
                .setAttribute("item.id", itemId)
                .startSpan();
        try {
            String payload = "{\"itemId\":\"" + itemId + "\",\"status\":\"" + status + "\"}";
            connection.publish("operations.updates", payload.getBytes(StandardCharsets.UTF_8));
            log.info("Published event to NATS for item {}", itemId);
        } finally {
            span.end();
        }
    }
}
