package com.paymentplatform.service.impl;

import com.paymentplatform.dto.request.CreatePaymentRequest;
import com.paymentplatform.dto.response.AnalyticsResponse;
import com.paymentplatform.dto.response.PaymentResponse;
import com.paymentplatform.dto.response.TransactionResponse;
import com.paymentplatform.entity.*;
import com.paymentplatform.exception.InvalidPaymentException;
import com.paymentplatform.exception.PaymentNotFoundException;
import com.paymentplatform.exception.PaymentProcessingException;
import com.paymentplatform.exception.RetryablePaymentException;
import com.paymentplatform.kafka.PaymentEvent;
import com.paymentplatform.repository.*;
import com.paymentplatform.util.CorrelationIdUtils;
import com.paymentplatform.util.PaymentReferenceGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final RetryLogRepository retryLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${app.payment.max-retry-attempts}")
    private int maxRetryAttempts;

    @Value("${app.payment.idempotency-ttl-seconds}")
    private long idempotencyTtlSeconds;

    @Value("${feature.cache.enabled:true}")
    private boolean cacheEnabled;

   
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public PaymentResponse createPayment(CreatePaymentRequest request, User user, String idempotencyKey) {
        user = requireAuthenticatedUser(user);
        // Idempotency check
        if (idempotencyKey != null) {
            String cachedResult = checkIdempotency(idempotencyKey, user.getId());
            if (cachedResult != null) {
                log.info("Returning cached result for idempotencyKey={}", idempotencyKey);
                // Return existing payment
                return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .map(this::mapToResponse)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for idempotency key"));
            }
        }

        String correlationId = CorrelationIdUtils.get();
        Timer.Sample timerSample = Timer.start(meterRegistry);

        Payment payment = Payment.builder()
            .paymentReference(PaymentReferenceGenerator.generate())
            .user(user)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(Payment.PaymentStatus.INITIATED)
            .paymentMethod(request.getPaymentMethod())
            .description(request.getDescription())
            .metadata(request.getMetadata())
            .idempotencyKey(idempotencyKey)
            .retryCount(0)
            .maxRetries(maxRetryAttempts)
            .initiatedAt(ZonedDateTime.now())
            .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created: ref={}, amount={} {}, userId={}, correlationId={}",
            payment.getPaymentReference(), payment.getAmount(), payment.getCurrency(),
            user.getId(), correlationId);

        try {
            // Cache idempotency
            if (idempotencyKey != null) {
                storeIdempotency(idempotencyKey, user.getId(), payment.getId().toString());
            }

            // Publish event
            PaymentEvent event = buildEvent(payment, PaymentEvent.EventTypes.PAYMENT_CREATED, correlationId);
            log.info("Kafka disabled - skipping payment event publishing");
            log.info("Payment event skipped because Kafka disabled: eventType={}, paymentId={}, correlationId={}",
                event.getEventType(), event.getPaymentId(), correlationId);
            // eventProducer.publishPaymentCreated(event);

            // Audit log
            saveAuditLog("PAYMENT", payment.getId(), "CREATED", null,
                Map.of("status", "INITIATED", "amount", payment.getAmount()), user.getId(), correlationId);

            String paymentMethodTag = request.getPaymentMethod() != null
                ? request.getPaymentMethod().name()
                : "UNKNOWN";
            meterRegistry.counter("payments.created",
                "currency", request.getCurrency(),
                "method", paymentMethodTag
            ).increment();

            processPayment(payment.getId(), correlationId);
        } catch (Exception ex) {
            log.warn("Non-fatal post-save payment work failed for paymentId={}, correlationId={}: {}",
                payment.getId(), correlationId, ex.getMessage(), ex);
        } finally {
            timerSample.stop(Timer.builder("payment.creation.time")
                .register(meterRegistry));
        }

        return mapToResponse(payment);
    }

    @Transactional
    @CircuitBreaker(name = "paymentProcessor", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentProcessor")
    public void processPayment(UUID paymentId, String correlationId) {
   


        try {
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

            if (payment.getStatus() != Payment.PaymentStatus.INITIATED &&
                payment.getStatus() != Payment.PaymentStatus.RETRYING) {
                log.info("Payment {} already in status {}, skipping processing", paymentId, payment.getStatus());
                return;
            }

            // Update to PROCESSING
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            PaymentEvent processingEvent = buildEvent(payment, PaymentEvent.EventTypes.PAYMENT_PROCESSING, correlationId);
            log.info("Payment event skipped because Kafka disabled: eventType={}, paymentId={}, correlationId={}",
                processingEvent.getEventType(), processingEvent.getPaymentId(), correlationId);
            // eventProducer.publishPaymentCreated(processingEvent);

            // Simulate payment gateway call
            String gatewayRef = simulateGatewayCall(payment);

            // SUCCESS
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setGatewayReference(gatewayRef);
            payment.setProcessedAt(ZonedDateTime.now());
            payment.setGatewayResponse(Map.of(
                "gateway_ref", gatewayRef,
                "processed_at", ZonedDateTime.now().toString(),
                "status", "SUCCESS"
            ));
            paymentRepository.save(payment);

            // Create transaction record
            Transaction transaction = Transaction.builder()
                .transactionReference(PaymentReferenceGenerator.generateTransactionRef())
                .payment(payment)
                .user(payment.getUser())
                .type(Transaction.TransactionType.DEBIT)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Payment processed: " + payment.getPaymentReference())
                .build();
            transactionRepository.save(transaction);

            log.info("Payment processed successfully: ref={}, gateway={}, correlationId={}",
                payment.getPaymentReference(), gatewayRef, correlationId);

            meterRegistry.counter("payments.success",
                "currency", payment.getCurrency(),
                "method", payment.getPaymentMethod().name()
            ).increment();

            saveAuditLog("PAYMENT", payment.getId(), "PROCESSED",
                Map.of("status", "PROCESSING"),
                Map.of("status", "SUCCESS", "gateway_ref", gatewayRef),
                payment.getUser().getId(), correlationId);

            PaymentEvent successEvent = buildEvent(payment, PaymentEvent.EventTypes.PAYMENT_SUCCESS, correlationId);
            log.info("Payment event skipped because Kafka disabled: eventType={}, paymentId={}, correlationId={}",
                successEvent.getEventType(), successEvent.getPaymentId(), correlationId);
            // eventProducer.publishPaymentProcessed(successEvent);

        } catch (RetryablePaymentException e) {
            handlePaymentFailure(paymentId, e.getMessage(), "RETRYABLE_ERROR", correlationId, true);
            throw e;
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            handlePaymentFailure(paymentId, e.getMessage(), "GATEWAY_ERROR", correlationId, true);
            throw new RetryablePaymentException("Payment gateway error: " + e.getMessage());
        } finally {
              
        }
    }

    public void processPaymentFallback(UUID paymentId, String correlationId, Exception ex) {
        log.error("Circuit breaker activated for payment {}: {}", paymentId, ex.getMessage());
        handlePaymentFailure(paymentId, "Payment service temporarily unavailable", "CIRCUIT_BREAKER_OPEN", correlationId, false);
        meterRegistry.counter("payments.circuit_breaker.activated").increment();
    }

    @Transactional
    @CacheEvict(value = {"payments", "analytics"}, allEntries = true)
    public PaymentResponse retryPayment(UUID paymentId, User user, String correlationId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (!payment.getUser().getId().equals(user.getId()) &&
            user.getRole() != User.UserRole.ADMIN) {
            throw new InvalidPaymentException("Access denied for payment: " + paymentId);
        }

        if (!payment.canRetry()) {
            throw new InvalidPaymentException(
                String.format("Payment cannot be retried. Status: %s, Attempts: %d/%d",
                    payment.getStatus(), payment.getRetryCount(), payment.getMaxRetries())
            );
        }

        payment.setStatus(Payment.PaymentStatus.RETRYING);
        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        RetryLog retryLog = RetryLog.builder()
            .payment(payment)
            .attemptNumber(payment.getRetryCount())
            .status(RetryLog.RetryStatus.ATTEMPTED)
            .retryAt(ZonedDateTime.now())
            .build();
        retryLogRepository.save(retryLog);

        log.info("Retrying payment: ref={}, attempt={}/{}, correlationId={}",
            payment.getPaymentReference(), payment.getRetryCount(),
            payment.getMaxRetries(), correlationId);

        meterRegistry.counter("payments.retried").increment();

        PaymentEvent retryEvent = buildEvent(payment, PaymentEvent.EventTypes.PAYMENT_RETRY, correlationId);
        retryEvent.setRetryCount(payment.getRetryCount());
        log.info("Payment event skipped because Kafka disabled: eventType={}, paymentId={}, correlationId={}",
            retryEvent.getEventType(), retryEvent.getPaymentId(), correlationId);
        // eventProducer.publishPaymentRetry(retryEvent);

        processPaymentAsync(payment.getId(), correlationId);

        return mapToResponse(payment);
    }

    @Transactional
    @CacheEvict(value = {"payments", "analytics"}, allEntries = true)
    public PaymentResponse reversePayment(UUID paymentId, User user, String correlationId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (!payment.getUser().getId().equals(user.getId()) &&
            user.getRole() != User.UserRole.ADMIN) {
            throw new InvalidPaymentException("Access denied for payment: " + paymentId);
        }

        if (!payment.canReverse()) {
            throw new InvalidPaymentException(
                "Payment cannot be reversed. Status: " + payment.getStatus()
            );
        }

        payment.setStatus(Payment.PaymentStatus.REVERSED);
        payment.setReversedAt(ZonedDateTime.now());
        paymentRepository.save(payment);

        // Create reversal transaction
        Transaction reversal = Transaction.builder()
            .transactionReference(PaymentReferenceGenerator.generateTransactionRef())
            .payment(payment)
            .user(payment.getUser())
            .type(Transaction.TransactionType.REVERSAL)
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(Transaction.TransactionStatus.COMPLETED)
            .description("Reversal for: " + payment.getPaymentReference())
            .build();
        transactionRepository.save(reversal);

        log.info("Payment reversed: ref={}, correlationId={}", payment.getPaymentReference(), correlationId);
        meterRegistry.counter("payments.reversed").increment();

        saveAuditLog("PAYMENT", payment.getId(), "REVERSED",
            Map.of("status", "SUCCESS"),
            Map.of("status", "REVERSED"),
            user.getId(), correlationId);

        PaymentEvent reversalEvent = buildEvent(payment, PaymentEvent.EventTypes.PAYMENT_REVERSED, correlationId);
        log.info("Payment event skipped because Kafka disabled: eventType={}, paymentId={}, correlationId={}",
            reversalEvent.getEventType(), reversalEvent.getPaymentId(), correlationId);
        // eventProducer.publishPaymentReversed(reversalEvent);

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "#paymentId")
    public PaymentResponse getPayment(UUID paymentId, User user) {
        user = requireAuthenticatedUser(user);
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (!payment.getUser().getId().equals(user.getId()) &&
            user.getRole() != User.UserRole.ADMIN) {
            throw new InvalidPaymentException("Access denied for payment: " + paymentId);
        }

        return mapToResponseWithDetails(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(User user, int page, int size, String status) {
        User authenticatedUser = requireAuthenticatedUser(user);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> payments;
        if (status != null && !status.isBlank()) {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            if (authenticatedUser.getRole() == User.UserRole.ADMIN) {
                payments = paymentRepository.findByStatusOrderByCreatedAtDesc(paymentStatus, pageable);
            } else {
                payments = paymentRepository.findAll(
                    (root, query, cb) -> cb.and(
                        cb.equal(root.get("user").get("id"), authenticatedUser.getId()),
                        cb.equal(root.get("status"), paymentStatus)
                    ), pageable
                );
            }
        } else {
            if (authenticatedUser.getRole() == User.UserRole.ADMIN) {
                payments = paymentRepository.findAll(pageable);
            } else {
                payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(authenticatedUser.getId(), pageable);
            }
        }

        return payments.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#days + ':' + (#user != null ? #user.id.toString() : 'anonymous')")
    public AnalyticsResponse getAnalytics(User user, int days) {
        user = requireAuthenticatedUser(user);
        ZonedDateTime since = ZonedDateTime.now().minusDays(days);
        boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

        List<Object[]> stats = isAdmin
            ? paymentRepository.getPaymentStatsSince(since)
            : paymentRepository.getPaymentStatsByUser(user.getId());

        long totalPayments = 0, successPayments = 0, failedPayments = 0, pendingPayments = 0, retriedPayments = 0;
        BigDecimal totalVolume = BigDecimal.ZERO, successVolume = BigDecimal.ZERO;
        Map<String, Long> statusBreakdown = new HashMap<>();

        for (Object[] row : stats) {
            Payment.PaymentStatus status = (Payment.PaymentStatus) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal volume = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

            totalPayments += count;
            totalVolume = totalVolume.add(volume);
            statusBreakdown.put(status.name(), count);

            switch (status) {
                case SUCCESS -> { successPayments = count; successVolume = volume; }
                case FAILED -> failedPayments = count;
                case RETRYING -> retriedPayments = count;
                case INITIATED, PROCESSING -> pendingPayments += count;
                default -> {}
            }
        }

        double successRate = totalPayments > 0
            ? (double) successPayments / totalPayments * 100
            : 0.0;

        double avgAmount = totalPayments > 0
            ? totalVolume.divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP).doubleValue()
            : 0.0;

        // Daily stats
        List<Object[]> dailyRaw = isAdmin
            ? paymentRepository.getDailyStats(since)
            : paymentRepository.getDailyStatsByUser(user.getId(), since);

        List<AnalyticsResponse.DailyStats> dailyStats = dailyRaw.stream()
            .map(row -> AnalyticsResponse.DailyStats.builder()
                .date(row[0] != null ? row[0].toString().substring(0, 10) : "")
                .total(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                .success(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                .failed(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                .volume(row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO)
                .build())
            .toList();

        // Recent activity
        Pageable recentPageable = PageRequest.of(0, 10);
        List<Payment> recentPayments = isAdmin
            ? paymentRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent()
            : paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), recentPageable).getContent();

        List<AnalyticsResponse.RecentActivity> recentActivity = recentPayments.stream()
            .map(p -> AnalyticsResponse.RecentActivity.builder()
                .paymentReference(p.getPaymentReference())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus().name())
                .paymentMethod(p.getPaymentMethod().name())
                .createdAt(p.getCreatedAt())
                .build())
            .toList();

        return AnalyticsResponse.builder()
            .totalPayments(totalPayments)
            .successfulPayments(successPayments)
            .failedPayments(failedPayments)
            .pendingPayments(pendingPayments)
            .retriedPayments(retriedPayments)
            .totalVolume(totalVolume)
            .successVolume(successVolume)
            .successRate(Math.round(successRate * 100.0) / 100.0)
            .averagePaymentAmount(avgAmount)
            .statusBreakdown(statusBreakdown)
            .dailyStats(dailyStats)
            .recentActivity(recentActivity)
            .generatedAt(ZonedDateTime.now())
            .build();
    }

    // ========== PRIVATE HELPERS ==========

    private void processPaymentAsync(UUID paymentId, String correlationId) {
        CompletableFuture.runAsync(() -> {
            try {
                processPayment(paymentId, correlationId);
            } catch (Exception e) {
                log.error("Async payment processing failed for paymentId={}: {}", paymentId, e.getMessage());
            }
        });
    }
private String simulateGatewayCall(Payment payment) throws InterruptedException {

    Thread.sleep(100 + (long) (Math.random() * 200));

    double random = Math.random();

    // 20% retryable failures
    if (random < 0.20) {
        throw new RetryablePaymentException(
                "Gateway timeout - connection refused"
        );
    }

    // 15% permanent failures
    if (random < 0.35) {
        throw new PaymentProcessingException(
                "Gateway error: insufficient funds",
                "INSUFFICIENT_FUNDS"
        );
    }

    // 65% success
    return "GW-" +
            UUID.randomUUID()
                    .toString()
                    .toUpperCase()
                    .substring(0, 12);
}

    @Transactional
private void handlePaymentFailure(UUID paymentId,
                                  String reason,
                                  String errorCode,
                                  String correlationId,
                                  boolean canRetry) {

    paymentRepository.findById(paymentId).ifPresent(payment -> {

        boolean shouldRetry =
                canRetry && payment.getRetryCount() < payment.getMaxRetries();

        if (shouldRetry) {

            payment.setStatus(Payment.PaymentStatus.RETRYING);
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setFailureReason(reason);

            paymentRepository.save(payment);

            RetryLog retryLog = RetryLog.builder()
                    .payment(payment)
                    .attemptNumber(payment.getRetryCount())
                    .status(RetryLog.RetryStatus.ATTEMPTED)
                    .retryAt(ZonedDateTime.now())
                    .errorMessage(reason)
                    .build();

            retryLogRepository.save(retryLog);

            log.warn(
                    "Retrying payment: ref={}, attempt={}/{}, reason={}",
                    payment.getPaymentReference(),
                    payment.getRetryCount(),
                    payment.getMaxRetries(),
                    reason
            );

            saveAuditLog(
                    "PAYMENT",
                    paymentId,
                    "RETRYING",
                    Map.of("status", "PROCESSING"),
                    Map.of(
                            "status", "RETRYING",
                            "attempt", payment.getRetryCount(),
                            "reason", reason
                    ),
                    payment.getUser().getId(),
                    correlationId
            );

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            processPaymentAsync(payment.getId(), correlationId);

            return;
        }

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(reason);

        paymentRepository.save(payment);

        meterRegistry.counter(
                "payments.failed",
                "reason", errorCode,
                "currency", payment.getCurrency()
        ).increment();

        saveAuditLog(
                "PAYMENT",
                paymentId,
                "FAILED",
                Map.of("status", "PROCESSING"),
                Map.of(
                        "status", "FAILED",
                        "reason", reason
                ),
                payment.getUser().getId(),
                correlationId
        );

        PaymentEvent failEvent = buildEvent(
                payment,
                PaymentEvent.EventTypes.PAYMENT_FAILED,
                correlationId
        );

        failEvent.setFailureReason(reason);

        log.info(
                "Payment event skipped because Kafka disabled: eventType={}, paymentId={}, correlationId={}",
                failEvent.getEventType(),
                failEvent.getPaymentId(),
                correlationId
        );

        log.error(
                "Payment failed: ref={}, reason={}, errorCode={}, correlationId={}",
                payment.getPaymentReference(),
                reason,
                errorCode,
                correlationId
        );
    });
}

    private String checkIdempotency(String key, UUID userId) {
        if (!cacheEnabled || redisTemplate.isEmpty()) {
            return null;
        }
        String redisKey = IDEMPOTENCY_PREFIX + userId + ":" + key;
        Object cached = redisTemplate.get().opsForValue().get(redisKey);
        return cached != null ? cached.toString() : null;
    }

    private void storeIdempotency(String key, UUID userId, String paymentId) {
        if (!cacheEnabled || redisTemplate.isEmpty()) {
            return;
        }
        String redisKey = IDEMPOTENCY_PREFIX + userId + ":" + key;
        redisTemplate.get().opsForValue().set(redisKey, paymentId, idempotencyTtlSeconds, TimeUnit.SECONDS);
    }

    private void saveAuditLog(String entityType, UUID entityId, String action,
                               Map<String, Object> oldValue, Map<String, Object> newValue,
                               UUID performedBy, String correlationId) {
        AuditLog auditLog = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .oldValue(oldValue)
            .newValue(newValue)
            .performedBy(performedBy)
            .correlationId(correlationId)
            .build();
        auditLogRepository.save(auditLog);
    }

    private PaymentEvent buildEvent(Payment payment, String eventType, String correlationId) {
        return PaymentEvent.of(
            eventType,
            payment.getId(),
            payment.getPaymentReference(),
            payment.getUser().getId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus().name(),
            correlationId
        );
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .paymentReference(payment.getPaymentReference())
            .userId(payment.getUser().getId())
            .username(payment.getUser().getUsername())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .paymentMethod(payment.getPaymentMethod().name())
            .description(payment.getDescription())
            .metadata(payment.getMetadata())
            .idempotencyKey(payment.getIdempotencyKey())
            .retryCount(payment.getRetryCount())
            .maxRetries(payment.getMaxRetries())
            .failureReason(payment.getFailureReason())
            .gatewayReference(payment.getGatewayReference())
            .canRetry(payment.canRetry())
            .canReverse(payment.canReverse())
            .initiatedAt(payment.getInitiatedAt())
            .processedAt(payment.getProcessedAt())
            .reversedAt(payment.getReversedAt())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }

    private PaymentResponse mapToResponseWithDetails(Payment payment) {
        PaymentResponse response = mapToResponse(payment);

        List<TransactionResponse> transactions = transactionRepository
            .findByPaymentIdOrderByCreatedAtDesc(payment.getId())
            .stream()
            .map(t -> TransactionResponse.builder()
                .id(t.getId())
                .transactionReference(t.getTransactionReference())
                .paymentId(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .userId(t.getUser().getId())
                .type(t.getType().name())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build())
            .toList();

        response.setTransactions(transactions);
        return response;
    }

    private User requireAuthenticatedUser(User user) {
        if (user == null) {
            throw new InvalidPaymentException("Authentication required");
        }
        return user;
    }

    @Scheduled(fixedDelay = 60000)
    public void processScheduledRetries() {
        log.debug("Checking for retryable payments...");
        List<Payment> retryablePayments = paymentRepository.findRetryablePayments();
        retryablePayments.forEach(p ->
            log.info("Found retryable payment: {}", p.getPaymentReference())
        );
    }
}
