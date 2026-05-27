package com.paymentplatform.exception;

public class PaymentProcessingException extends RuntimeException {
    private final String errorCode;

    public PaymentProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentProcessingException(String message) {
        super(message);
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
