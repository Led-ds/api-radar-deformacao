package com.ass.br.apiradar.shared.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint de Health Check padronizado da API v1.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckRestController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "Radar++",
                "version", "0.0.1-MVP",
                "timestamp", Instant.now().toString(),
                "profile", System.getProperty("spring.profiles.active", "dev")
        ));
    }
}
