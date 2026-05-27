package com.paymentplatform.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventId;
    private String eventType;
    private UUID paymentId;
    private String paymentReference;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String failureReason;
    private Integer retryCount;
    private String correlationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime timestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime scheduledRetryAt;

    public static PaymentEvent of(String eventType, UUID paymentId, String paymentReference,
                                   UUID userId, BigDecimal amount, String currency,
                                   String status, String correlationId) {
        return PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .paymentId(paymentId)
            .paymentReference(paymentReference)
            .userId(userId)
            .amount(amount)
            .currency(currency)
            .status(status)
            .correlationId(correlationId)
            .timestamp(ZonedDateTime.now())
            .build();
    }

    public static final class EventTypes {
        public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
        public static final String PAYMENT_PROCESSING = "PAYMENT_PROCESSING";
        public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String PAYMENT_RETRY = "PAYMENT_RETRY";
        public static final String PAYMENT_REVERSED = "PAYMENT_REVERSED";
        public static final String PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT";
        public static final String NOTIFICATION_SEND = "NOTIFICATION_SEND";
        private EventTypes() {}
    }
}
