package com.paymentplatform.exception;

import com.paymentplatform.dto.response.ApiResponse;
import com.paymentplatform.util.CorrelationIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message("Validation failed")
            .correlationId(CorrelationIdUtils.get())
            .error(ApiResponse.ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .validationErrors(errors)
                .build())
            .timestamp(java.time.ZonedDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentNotFound(PaymentNotFoundException ex) {
        log.warn("Payment not found: {} [correlationId={}]", ex.getMessage(), CorrelationIdUtils.get());
        return buildErrorResponse(ex.getMessage(), "PAYMENT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPayment(InvalidPaymentException ex) {
        log.warn("Invalid payment: {} [correlationId={}]", ex.getMessage(), CorrelationIdUtils.get());
        return buildErrorResponse(ex.getMessage(), "INVALID_PAYMENT", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentProcessing(PaymentProcessingException ex) {
        log.error("Payment processing error: {} [correlationId={}]", ex.getMessage(), CorrelationIdUtils.get());
        return buildErrorResponse(ex.getMessage(), ex.getErrorCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateRequest(DuplicateRequestException ex) {
        log.info("Duplicate request detected [correlationId={}]", CorrelationIdUtils.get());
        return buildErrorResponse(ex.getMessage(), "DUPLICATE_REQUEST", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse("Invalid username or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UsernameNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse("Access denied", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation [correlationId={}]: {}", CorrelationIdUtils.get(), ex.getMostSpecificCause().getMessage());
        return buildErrorResponse("Invalid or duplicate data", "DATA_INTEGRITY_VIOLATION", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime error [correlationId={}]: {}", CorrelationIdUtils.get(), ex.getMessage(), ex);
        return buildErrorResponse("An unexpected error occurred", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error [correlationId={}]: {}", CorrelationIdUtils.get(), ex.getMessage(), ex);
        return buildErrorResponse("An unexpected error occurred", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(String message, String code, HttpStatus status) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message(message)
            .correlationId(CorrelationIdUtils.get())
            .error(ApiResponse.ErrorDetails.builder().code(code).build())
            .timestamp(java.time.ZonedDateTime.now())
            .build();
        return ResponseEntity.status(status).body(response);
    }
}
