package com.paymentplatform.exception;

public class DuplicateRequestException extends RuntimeException {
    private final String cachedResponse;

    public DuplicateRequestException(String message, String cachedResponse) {
        super(message);
        this.cachedResponse = cachedResponse;
    }

    public String getCachedResponse() {
        return cachedResponse;
    }
}
