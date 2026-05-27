package com.paymentplatform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "feature.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Value("${kafka.topics.payment-created}")
    private String paymentCreatedTopic;

    @Value("${kafka.topics.payment-processed}")
    private String paymentProcessedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    @Value("${kafka.topics.payment-retry}")
    private String paymentRetryTopic;

    @Value("${kafka.topics.payment-reversed}")
    private String paymentReversedTopic;

    @Value("${kafka.topics.notification}")
    private String notificationTopic;

    @Value("${kafka.topics.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(
            "bootstrap.servers", bootstrapServers
        ));
    }

    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name(paymentCreatedTopic)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000") // 7 days
            .build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(paymentProcessedTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(paymentFailedTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentRetryTopic() {
        return TopicBuilder.name(paymentRetryTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentReversedTopic() {
        return TopicBuilder.name(paymentReversedTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(notificationTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic)
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30 days
            .build();
    }
}
