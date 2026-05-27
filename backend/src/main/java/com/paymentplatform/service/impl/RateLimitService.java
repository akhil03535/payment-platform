package com.paymentplatform.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    @Value("${app.payment.rate-limit-requests:100}")
    private int maxRequests;

    @Value("${app.payment.rate-limit-window-seconds:60}")
    private int windowSeconds;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public boolean isAllowed(String identifier) {
        if (redisTemplate.isEmpty()) {
            return true;
        }
        String key = RATE_LIMIT_PREFIX + identifier;
        Long current = redisTemplate.get().opsForValue().increment(key);
        if (current == 1) {
            redisTemplate.get().expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        if (current > maxRequests) {
            log.warn("Rate limit exceeded for: {}", identifier);
            return false;
        }
        return true;
    }

    public long getRemainingRequests(String identifier) {
        if (redisTemplate.isEmpty()) {
            return maxRequests;
        }
        String key = RATE_LIMIT_PREFIX + identifier;
        Object current = redisTemplate.get().opsForValue().get(key);
        if (current == null) return maxRequests;
        return Math.max(0, maxRequests - Long.parseLong(current.toString()));
    }
}
