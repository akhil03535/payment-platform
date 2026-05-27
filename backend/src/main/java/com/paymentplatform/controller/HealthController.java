package com.paymentplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", ZonedDateTime.now());
        health.put("service", "payment-platform");
        health.put("version", "1.0.0");

        // DB check
        try (Connection conn = dataSource.getConnection()) {
            health.put("database", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("database", "DOWN");
        }

        // Redis check
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().getConnectionFactory().getConnection().ping();
                health.put("redis", "UP");
            } catch (Exception e) {
                health.put("redis", "DOWN");
            }
        } else {
            health.put("redis", "DISABLED");
        }

        return ResponseEntity.ok(health);
    }
}
