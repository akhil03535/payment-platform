package com.paymentplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetryLogResponse {
    private UUID id;
    private UUID paymentId;
    private Integer attemptNumber;
    private String status;
    private String errorMessage;
    private String errorCode;
    private ZonedDateTime retryAt;
    private ZonedDateTime nextRetryAt;
    private ZonedDateTime createdAt;
}
