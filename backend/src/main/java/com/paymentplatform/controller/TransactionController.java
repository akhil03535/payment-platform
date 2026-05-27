package com.paymentplatform.controller;

import com.paymentplatform.dto.response.ApiResponse;
import com.paymentplatform.dto.response.TransactionResponse;
import com.paymentplatform.entity.Transaction;
import com.paymentplatform.entity.User;
import com.paymentplatform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TransactionResponse> transactions;

        if (user.getRole() == User.UserRole.ADMIN) {
            transactions = transactionRepository.findAll(pageable).map(this::mapToResponse);
        } else {
            transactions = transactionRepository
                    .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                    .map(this::mapToResponse);
        }

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal User user) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId()) &&
                user.getRole() != User.UserRole.ADMIN) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied", "FORBIDDEN"));
        }

        return ResponseEntity.ok(ApiResponse.success(mapToResponse(transaction)));
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .transactionReference(t.getTransactionReference())
                .paymentId(t.getPayment().getId())
                .paymentReference(t.getPayment().getPaymentReference())
                .userId(t.getUser().getId())
                .type(t.getType().name())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
