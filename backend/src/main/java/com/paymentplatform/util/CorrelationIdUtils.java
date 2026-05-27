package com.paymentplatform.util;

import org.slf4j.MDC;

import java.util.UUID;

public class CorrelationIdUtils {

    private static final String CORRELATION_ID_KEY = "correlationId";

    public static String generate() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static void set(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    public static String get() {
        String id = MDC.get(CORRELATION_ID_KEY);
        return id != null ? id : generate();
    }

    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
    }

    private CorrelationIdUtils() {}
}
