package com.paymentplatform.service;

import com.paymentplatform.dto.request.CreatePaymentRequest;
import com.paymentplatform.dto.response.PaymentResponse;
import com.paymentplatform.entity.Payment;
import com.paymentplatform.entity.User;
import com.paymentplatform.exception.InvalidPaymentException;
import com.paymentplatform.exception.PaymentNotFoundException;
import com.paymentplatform.kafka.producer.PaymentEventProducer;
import com.paymentplatform.repository.*;
import com.paymentplatform.service.impl.PaymentService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private RetryLogRepository retryLogRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentEventProducer eventProducer;
    //ock private RedissonClient redissonClient;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Payment testPayment;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(paymentService, "meterRegistry", meterRegistry);
        ReflectionTestUtils.setField(paymentService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(paymentService, "idempotencyTtlSeconds", 86400L);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        testPayment = Payment.builder()
                .id(UUID.randomUUID())
                .paymentReference("PAY-20240101-ABCD1234")
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(Payment.PaymentStatus.INITIATED)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .retryCount(0)
                .maxRetries(3)
                .initiatedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create payment successfully")
    void shouldCreatePaymentSuccessfully() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100.00"), "USD",
                Payment.PaymentMethod.CARD, "Test payment", null
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(auditLogRepository.save(any())).thenReturn(null);
        doNothing().when(eventProducer).publishPaymentCreated(any());

        PaymentResponse response = paymentService.createPayment(request, testUser, null);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");
        verify(paymentRepository).save(any(Payment.class));
        verify(eventProducer).publishPaymentCreated(any());
    }

    @Test
    @DisplayName("Should return cached payment for duplicate idempotency key")
    void shouldReturnCachedPaymentForDuplicateIdempotencyKey() {
        String idempotencyKey = "test-key-123";
        String cachedPaymentId = testPayment.getId().toString();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cachedPaymentId);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(testPayment));

        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100.00"), "USD",
                Payment.PaymentMethod.CARD, "Test", null
        );

        PaymentResponse response = paymentService.createPayment(request, testUser, idempotencyKey);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testPayment.getId());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(paymentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(nonExistentId, testUser))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("Should deny access to other user payment")
    void shouldDenyAccessToOtherUserPayment() {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("other")
                .role(User.UserRole.USER)
                .build();

        when(paymentRepository.findById(testPayment.getId()))
                .thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() -> paymentService.getPayment(testPayment.getId(), otherUser))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Should not retry payment that exceeded max retries")
    void shouldNotRetryPaymentThatExceededMaxRetries() {
        testPayment.setStatus(Payment.PaymentStatus.FAILED);
        testPayment.setRetryCount(3);
        testPayment.setMaxRetries(3);

        when(paymentRepository.findById(testPayment.getId()))
                .thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() ->
                paymentService.retryPayment(testPayment.getId(), testUser, "corr-id"))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("cannot be retried");
    }

    @Test
    @DisplayName("Should not reverse non-successful payment")
    void shouldNotReverseNonSuccessfulPayment() {
        testPayment.setStatus(Payment.PaymentStatus.FAILED);

        when(paymentRepository.findById(testPayment.getId()))
                .thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() ->
                paymentService.reversePayment(testPayment.getId(), testUser, "corr-id"))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("cannot be reversed");
    }

    @Test
    @DisplayName("Should get paginated payments for user")
    void shouldGetPaginatedPaymentsForUser() {
        Page<Payment> mockPage = new PageImpl<>(List.of(testPayment));
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(
                eq(testUser.getId()), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<PaymentResponse> result = paymentService.getPayments(testUser, 0, 20, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testPayment.getId());
    }

    @Test
    @DisplayName("Admin should see all payments")
    void adminShouldSeeAllPayments() {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role(User.UserRole.ADMIN)
                .build();

        Page<Payment> mockPage = new PageImpl<>(List.of(testPayment));
        when(paymentRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

        Page<PaymentResponse> result = paymentService.getPayments(adminUser, 0, 20, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}
