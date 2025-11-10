package com.example.gateway.websocket;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class GatewaySocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewaySocketHandler.class);
    private final ObservationRegistry observationRegistry;

    public GatewaySocketHandler(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Observation.createNotStarted("gateway.socket.message", observationRegistry).observe(() -> {
            try {
                session.sendMessage(new TextMessage("Hello " + message.getPayload()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
