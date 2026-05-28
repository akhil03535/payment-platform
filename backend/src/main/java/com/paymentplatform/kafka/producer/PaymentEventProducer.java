package com.paymentplatform.kafka.producer;

import com.paymentplatform.kafka.PaymentEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
// @Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    // private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

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

    public void publishPaymentCreated(PaymentEvent event) {
        publishEvent(paymentCreatedTopic, event);
    }

    public void publishPaymentProcessed(PaymentEvent event) {
        publishEvent(paymentProcessedTopic, event);
    }

    public void publishPaymentFailed(PaymentEvent event) {
        publishEvent(paymentFailedTopic, event);
    }

    public void publishPaymentRetry(PaymentEvent event) {
        publishEvent(paymentRetryTopic, event);
    }

    public void publishPaymentReversed(PaymentEvent event) {
        publishEvent(paymentReversedTopic, event);
    }

    public void publishNotification(PaymentEvent event) {
        publishEvent(notificationTopic, event);
    }

    public void publishToDlq(PaymentEvent event, String reason) {
        event.setFailureReason(reason);
        publishEvent(dlqTopic, event);
        meterRegistry.counter("kafka.dlq.messages",
            "topic", dlqTopic,
            "event_type", event.getEventType()
        ).increment();
    }

    // private void publishEvent(String topic, PaymentEvent event) {
    //     String key = event.getPaymentId() != null ? event.getPaymentId().toString() : event.getEventId();

    //     CompletableFuture<SendResult<String, PaymentEvent>> future =
    //         kafkaTemplate.send(topic, key, event);

    //     future.whenComplete((result, ex) -> {
    //         if (ex == null) {
    //             log.info("Published event [{}] to topic [{}] partition [{}] offset [{}] correlationId [{}]",
    //                 event.getEventType(), topic,
    //                 result.getRecordMetadata().partition(),
    //                 result.getRecordMetadata().offset(),
    //                 event.getCorrelationId());

    //             meterRegistry.counter("kafka.events.published",
    //                 "topic", topic,
    //                 "event_type", event.getEventType()
    //             ).increment();
    //         } else {
    //             log.error("Failed to publish event [{}] to topic [{}]: {}",
    //                 event.getEventType(), topic, ex.getMessage());

    //             meterRegistry.counter("kafka.events.failed",
    //                 "topic", topic,
    //                 "event_type", event.getEventType()
    //             ).increment();
    //         }
    //     }
           
    
    // );
    // }

    private void publishEvent(String topic, PaymentEvent event) {

    log.info("Kafka disabled locally. Event skipped: {}",
            event.getEventType());

}
}
