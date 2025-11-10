package com.example.operation.websocket;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class OperationNatsSubscriber {

    private static final Logger log = LoggerFactory.getLogger(OperationNatsSubscriber.class);

    private final Connection connection;
    private final OperationSocketPublisher publisher;
    private final ObservationRegistry observationRegistry;

    private Dispatcher dispatcher;

    public OperationNatsSubscriber(Connection connection,
                                   OperationSocketPublisher publisher,
                                   ObservationRegistry observationRegistry) {
        this.connection = connection;
        this.publisher = publisher;
        this.observationRegistry = observationRegistry;
    }

    @PostConstruct
    public void subscribe() {
        dispatcher = connection.createDispatcher(message -> Observation
                .createNotStarted("operation.nats.socket.forward", observationRegistry)
                .observe(() -> {
                    String payload = new String(message.getData(), StandardCharsets.UTF_8);
                    log.debug("Forwarding NATS payload to WebSocket clients: {}", payload);
                    publisher.publish(payload);
                }));
        dispatcher.subscribe("operations.updates");
        log.info("Subscribed to NATS subject operations.updates for WebSocket broadcast");
    }

    @PreDestroy
    public void cleanup() {
        if (dispatcher != null) {
            try {
                dispatcher.unsubscribe("operations.updates");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
