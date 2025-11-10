package com.example.gateway;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayRestController {

    private final ObservationRegistry observationRegistry;

    public GatewayRestController(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping(@RequestParam(defaultValue = "World") String name) {
        return Observation.createNotStarted("gateway.http.ping", observationRegistry)
                .observe(() -> ResponseEntity.ok(Map.of(
                        "message", "Hello " + name,
                        "channel", "REST"
                )));
    }
}
