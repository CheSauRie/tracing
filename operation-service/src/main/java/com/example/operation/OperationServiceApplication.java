package com.example.operation;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

@SpringBootApplication
public class OperationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperationServiceApplication.class, args);
    }
}

@Configuration
class OperationServiceConfiguration {

    @Bean
    public WebClient inventoryWebClient(@Value("${inventory.service.http-base-url:http://localhost:8081}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
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

@Configuration
class OperationTelemetryConfiguration {

    @Bean
    @Primary
    public OpenTelemetrySdk openTelemetry(@Value("${management.otlp.tracing.endpoint:http://localhost:4317}") String endpoint,
                                          @Value("${spring.application.name:operation-service}") String serviceName) {
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
        return openTelemetrySdk.getTracer("operation-service");
    }
}
