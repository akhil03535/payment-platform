package com.paymentplatform.dto.request;

import com.paymentplatform.entity.Payment;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount cannot exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase (e.g., USD, EUR, INR)")
    private String currency;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Map<String, Object> metadata;
}
