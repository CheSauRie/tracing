package com.example.operation.websocket;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final ObservationRegistry observationRegistry;
    private final OperationSocketPublisher publisher;

    public WebSocketConfiguration(ObservationRegistry observationRegistry, OperationSocketPublisher publisher) {
        this.observationRegistry = observationRegistry;
        this.publisher = publisher;
    }

    @Bean
    public OperationSocketHandler operationSocketHandler() {
        return new OperationSocketHandler(observationRegistry, publisher);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(operationSocketHandler(), "/ws/operations").setAllowedOrigins("*");
    }
}
