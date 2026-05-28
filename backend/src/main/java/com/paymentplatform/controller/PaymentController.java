package com.paymentplatform.controller;

import com.paymentplatform.dto.request.CreatePaymentRequest;
import com.paymentplatform.dto.response.AnalyticsResponse;
import com.paymentplatform.dto.response.ApiResponse;
import com.paymentplatform.dto.response.PaymentResponse;
import com.paymentplatform.entity.User;
import com.paymentplatform.service.impl.PaymentService;
import com.paymentplatform.util.CorrelationIdUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required", "UNAUTHORIZED"));
        }

        PaymentResponse response = paymentService.createPayment(request, user, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment initiated successfully"));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required", "UNAUTHORIZED"));
        }

        PaymentResponse response = paymentService.getPayment(paymentId, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required", "UNAUTHORIZED"));
        }

        Page<PaymentResponse> payments = paymentService.getPayments(user, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required", "UNAUTHORIZED"));
        }

        String correlationId = CorrelationIdUtils.get();
        PaymentResponse response = paymentService.retryPayment(paymentId, user, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment retry initiated"));
    }

    @PostMapping("/{paymentId}/reverse")
    public ResponseEntity<ApiResponse<PaymentResponse>> reversePayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required", "UNAUTHORIZED"));
        }

        String correlationId = CorrelationIdUtils.get();
        PaymentResponse response = paymentService.reversePayment(paymentId, user, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment reversed successfully"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "30") int days) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required", "UNAUTHORIZED"));
        }

        AnalyticsResponse analytics = paymentService.getAnalytics(user, days);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
