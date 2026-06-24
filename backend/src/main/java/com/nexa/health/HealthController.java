package com.nexa.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Egyszerű health végpont, amit a frontend kezdőoldala lekérdez (1. kártya).
 * Visszaadja a szolgáltatás állapotát, nevét és verzióját.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "nexa-backend",
                "version", "0.1.0",
                "timestamp", Instant.now().toString()
        );
    }
}
