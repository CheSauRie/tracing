package com.example.inventory;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryRestController {

    private final ObservationRegistry observationRegistry;

    public InventoryRestController(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @GetMapping("/status/{itemId}")
    public ResponseEntity<Map<String, String>> status(@PathVariable String itemId) {
        return Observation.createNotStarted("inventory.http.status", observationRegistry)
                .observe(() -> ResponseEntity.ok(Map.of(
                        "itemId", itemId,
                        "status", "AVAILABLE",
                        "source", "inventory-service"
                )));
    }
}
