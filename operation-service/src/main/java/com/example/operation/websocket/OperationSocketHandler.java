package com.example.operation.websocket;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;

public class OperationSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OperationSocketHandler.class);

    private final ObservationRegistry observationRegistry;
    private final OperationSocketPublisher publisher;

    public OperationSocketHandler(ObservationRegistry observationRegistry, OperationSocketPublisher publisher) {
        this.observationRegistry = observationRegistry;
        this.publisher = publisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        publisher.register(session);
        session.sendMessage(new TextMessage("Connected to operation-service at " + Instant.now()));
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        publisher.unregister(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Observation.createNotStarted("operation.socket.message", observationRegistry).observe(() -> {
            log.debug("Received WebSocket message '{}' from {}", message.getPayload(), session.getId());
            publisher.publish("ACK: " + message.getPayload());
        });
    }
}
