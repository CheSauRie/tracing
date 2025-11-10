package com.example.inventory;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Message;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.ResourceAttributes;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
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
    private final Tracer tracer;

    InventoryNatsConfiguration(Connection connection, Tracer tracer) {
        this.connection = connection;
        this.dispatcher = connection.createDispatcher();
        this.tracer = tracer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        dispatcher.subscribe("operations.updates", this::handleMessage);
        log.info("Subscribed to operations.updates subject on NATS");
    }

    private void handleMessage(Message message) {
        Span span = tracer.spanBuilder("inventory-service.nats.receive").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            log.info("Received NATS message: {}", message.getData() == null ? "<empty>" : new String(message.getData()));
        } finally {
            span.end();
        }
    }

    @PreDestroy
    public void shutdown() {
        dispatcher.unsubscribe("operations.updates");
        connection.close();
    }
}

@Configuration
class InventoryTelemetryConfiguration {

    @Bean
    @Primary
    public OpenTelemetrySdk openTelemetry(@Value("${management.otlp.tracing.endpoint:http://localhost:4317}") String endpoint,
                                          @Value("${spring.application.name:inventory-service}") String serviceName) {
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();

        Resource resource = Resource.getDefault().merge(Resource.create(io.opentelemetry.api.common.Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .build()));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }

    @Bean
    public Tracer tracer(OpenTelemetrySdk openTelemetrySdk) {
        return openTelemetrySdk.getTracer("inventory-service");
    }

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
