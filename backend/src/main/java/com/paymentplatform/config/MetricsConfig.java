package com.paymentplatform.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter paymentsCreatedCounter(MeterRegistry registry) {
        return Counter.builder("payments.created.total")
            .description("Total number of payments created")
            .register(registry);
    }

    @Bean
    public Counter paymentsSuccessCounter(MeterRegistry registry) {
        return Counter.builder("payments.success.total")
            .description("Total number of successful payments")
            .register(registry);
    }

    @Bean
    public Counter paymentsFailedCounter(MeterRegistry registry) {
        return Counter.builder("payments.failed.total")
            .description("Total number of failed payments")
            .register(registry);
    }

    @Bean
    public Counter paymentsRetriedCounter(MeterRegistry registry) {
        return Counter.builder("payments.retried.total")
            .description("Total number of retried payments")
            .register(registry);
    }

    @Bean
    public Counter paymentsReversedCounter(MeterRegistry registry) {
        return Counter.builder("payments.reversed.total")
            .description("Total number of reversed payments")
            .register(registry);
    }

    @Bean
    public Timer paymentProcessingTimer(MeterRegistry registry) {
        return Timer.builder("payment.processing.duration")
            .description("Time taken to process a payment")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
}
