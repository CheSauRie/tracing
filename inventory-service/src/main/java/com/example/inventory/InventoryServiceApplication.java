package com.example.inventory;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.time.Duration;

@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}

@Configuration
class InventoryNatsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InventoryNatsConfiguration.class);

    private final Connection connection;
    private final Dispatcher dispatcher;
    private final ObservationRegistry observationRegistry;

    InventoryNatsConfiguration(Connection connection, ObservationRegistry observationRegistry) {
        this.connection = connection;
        this.dispatcher = connection.createDispatcher();
        this.observationRegistry = observationRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        dispatcher.subscribe("operations.updates", this::handleMessage);
        log.info("Subscribed to operations.updates subject on NATS");
    }

    private void handleMessage(Message message) {
        Observation observation = Observation.createNotStarted("inventory.nats.receive", observationRegistry);
        observation.observe(() -> {
            log.info("Received NATS message: {}", message.getData() == null ? "<empty>" : new String(message.getData()));
        });
    }

    @PreDestroy
    public void shutdown() {
        dispatcher.unsubscribe("operations.updates");
        connection.close();
    }
}

@Configuration
class InventoryMessagingConfiguration {

    @Bean
    public Connection natsConnection(@Value("${nats.url:nats://localhost:4222}") String url) throws IOException, InterruptedException {
        Options options = new Options.Builder()
                .server(url)
                .connectionTimeout(Duration.ofSeconds(2))
                .maxReconnects(-1)
                .build();
        return Nats.connect(options);
    }
}
