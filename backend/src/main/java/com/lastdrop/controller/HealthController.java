package com.lastdrop.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tiny endpoint that answers "I'm alive".
 *
 * The React frontend calls GET /api/health to prove that the two halves of the
 * application can talk to each other. Hosting platforms (Render, etc.) can also
 * call it to check whether the backend is running.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", applicationName,
                "serverTime", Instant.now().toString()
        );
    }
}
