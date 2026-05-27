package com.paymentplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private UUID id;
    private String transactionReference;
    private UUID paymentId;
    private String paymentReference;
    private UUID userId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String status;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private ZonedDateTime createdAt;
}
