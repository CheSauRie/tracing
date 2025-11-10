package com.example.operation.websocket;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OperationSocketPublisher {

    private static final Logger log = LoggerFactory.getLogger(OperationSocketPublisher.class);

    private final ObservationRegistry observationRegistry;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public OperationSocketPublisher(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket session registered: {}", session.getId());
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
        log.info("WebSocket session removed: {}", session.getId());
    }

    public void publish(String payload) {
        Observation.createNotStarted("operation.socket.publish", observationRegistry)
                .observe(() -> sessions.forEach(session -> send(session, payload)));
    }

    private void send(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ex) {
            log.warn("Failed to send WebSocket message to {}", session.getId(), ex);
            try {
                session.close();
            } catch (IOException ioException) {
                log.debug("Failed to close WebSocket session {}", session.getId(), ioException);
            }
            sessions.remove(session);
        }
    }
}
