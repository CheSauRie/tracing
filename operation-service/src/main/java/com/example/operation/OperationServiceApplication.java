package com.example.operation;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
