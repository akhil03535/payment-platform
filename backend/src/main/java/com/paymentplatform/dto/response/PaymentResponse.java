package com.paymentplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private UUID id;
    private String paymentReference;
    private UUID userId;
    private String username;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String description;
    private Map<String, Object> metadata;
    private String idempotencyKey;
    private Integer retryCount;
    private Integer maxRetries;
    private String failureReason;
    private String gatewayReference;
    private boolean canRetry;
    private boolean canReverse;
    private ZonedDateTime initiatedAt;
    private ZonedDateTime processedAt;
    private ZonedDateTime reversedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private List<RetryLogResponse> retryLogs;
    private List<TransactionResponse> transactions;
}
