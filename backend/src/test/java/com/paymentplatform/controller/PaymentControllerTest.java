package com.paymentplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.dto.request.CreatePaymentRequest;
import com.paymentplatform.dto.response.PaymentResponse;
import com.paymentplatform.entity.Payment;
import com.paymentplatform.entity.User;
import com.paymentplatform.service.impl.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Integration Tests")
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private PaymentService paymentService;

    private PaymentResponse mockPaymentResponse;

    @BeforeEach
    void setUp() {
        mockPaymentResponse = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .paymentReference("PAY-20240101-ABCD1234")
                .userId(UUID.randomUUID())
                .username("testuser")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .status("INITIATED")
                .paymentMethod("CARD")
                .retryCount(0)
                .maxRetries(3)
                .canRetry(false)
                .canReverse(false)
                .initiatedAt(ZonedDateTime.now())
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/payments - Should create payment successfully")
    void shouldCreatePaymentSuccessfully() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("150.00"), "USD",
                Payment.PaymentMethod.CARD, "Test payment", null
        );

        when(paymentService.createPayment(any(), any(), any()))
                .thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-20240101-ABCD1234"))
                .andExpect(jsonPath("$.data.amount").value(150.00))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.status").value("INITIATED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/payments - Should fail with invalid amount")
    void shouldFailWithInvalidAmount() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("-50.00"), "USD",
                Payment.PaymentMethod.CARD, "Bad payment", null
        );

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/payments - Should fail with invalid currency")
    void shouldFailWithInvalidCurrency() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100.00"), "us",  // lowercase – invalid
                Payment.PaymentMethod.CARD, null, null
        );

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("GET /api/payments - Should return paginated payments")
    void shouldReturnPaginatedPayments() throws Exception {
        when(paymentService.getPayments(any(), eq(0), eq(20), isNull()))
                .thenReturn(new PageImpl<>(List.of(mockPaymentResponse)));

        mockMvc.perform(get("/api/payments")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].paymentReference")
                        .value("PAY-20240101-ABCD1234"));
    }

    @Test
    @DisplayName("GET /api/payments - Should return 401 without auth")
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/payments/{id}/retry - Should retry payment")
    void shouldRetryPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        mockPaymentResponse.setStatus("RETRYING");
        mockPaymentResponse.setRetryCount(1);

        when(paymentService.retryPayment(eq(paymentId), any(), any()))
                .thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/payments/{id}/retry", paymentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RETRYING"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/payments/{id}/reverse - Should reverse payment")
    void shouldReversePayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        mockPaymentResponse.setStatus("REVERSED");

        when(paymentService.reversePayment(eq(paymentId), any(), any()))
                .thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/payments/{id}/reverse", paymentId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVERSED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/payments - Should accept Idempotency-Key header")
    void shouldAcceptIdempotencyKeyHeader() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("100.00"), "USD",
                Payment.PaymentMethod.CARD, null, null
        );

        when(paymentService.createPayment(any(), any(), eq("my-unique-key-123")))
                .thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", "my-unique-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(paymentService).createPayment(any(), any(), eq("my-unique-key-123"));
    }
}
