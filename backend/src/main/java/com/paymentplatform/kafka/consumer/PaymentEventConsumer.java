package com.paymentplatform.kafka.consumer;

import com.paymentplatform.kafka.PaymentEvent;
import com.paymentplatform.kafka.producer.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
// @Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentEventProducer eventProducer;

    /* @KafkaListener(
        topics = "${kafka.topics.payment-failed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    ) */
    public void handlePaymentFailed(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
        PaymentEvent event = record.value();
        try {
            log.info("Received payment failed event: paymentId={}, correlationId={}",
                event.getPaymentId(), event.getCorrelationId());

            // Publish notification for failed payment
            PaymentEvent notificationEvent = PaymentEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(PaymentEvent.EventTypes.NOTIFICATION_SEND)
                .paymentId(event.getPaymentId())
                .paymentReference(event.getPaymentReference())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .status(event.getStatus())
                .failureReason(event.getFailureReason())
                .correlationId(event.getCorrelationId())
                .timestamp(java.time.ZonedDateTime.now())
                .build();

            eventProducer.publishNotification(notificationEvent);
            ack.acknowledge();
            log.info("Processed payment failed event for paymentId={}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Error processing payment failed event: {}", e.getMessage(), e);
            eventProducer.publishToDlq(event, "Consumer processing error: " + e.getMessage());
            ack.acknowledge();
        }
    }

    /* @KafkaListener(
        topics = "${kafka.topics.payment-processed}",
        groupId = "${spring.kafka.consumer.group-id}"
    ) */
    public void handlePaymentProcessed(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
        PaymentEvent event = record.value();
        try {
            log.info("Payment successfully processed: paymentRef={}, amount={} {}",
                event.getPaymentReference(), event.getAmount(), event.getCurrency());

            // Publish success notification
            eventProducer.publishNotification(PaymentEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(PaymentEvent.EventTypes.NOTIFICATION_SEND)
                .paymentId(event.getPaymentId())
                .paymentReference(event.getPaymentReference())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .status("SUCCESS")
                .correlationId(event.getCorrelationId())
                .timestamp(java.time.ZonedDateTime.now())
                .build());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment success event: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }

    /* @KafkaListener(
        topics = "${kafka.topics.notification}",
        groupId = "${spring.kafka.consumer.group-id}-notifications"
    ) */
    @Async("kafkaTaskExecutor")
    public void handleNotification(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
        PaymentEvent event = record.value();
        try {
            log.info("Processing notification for userId={}, paymentRef={}, status={}",
                event.getUserId(), event.getPaymentReference(), event.getStatus());
            // In production: send email, SMS, push notification
            // For now: structured log as notification
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process notification: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }

    /* @KafkaListener(
        topics = "${kafka.topics.dlq}",
        groupId = "${spring.kafka.consumer.group-id}-dlq"
    ) */
    public void handleDlqMessage(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
        PaymentEvent event = record.value();
        log.error("DLQ message received: eventType={}, paymentId={}, reason={}, correlationId={}",
            event.getEventType(), event.getPaymentId(),
            event.getFailureReason(), event.getCorrelationId());
        // In production: alert, manual review queue, etc.
        ack.acknowledge();
    }
}
