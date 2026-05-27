package com.paymentplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "retry_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetryStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "retry_at")
    private ZonedDateTime retryAt;

    @Column(name = "next_retry_at")
    private ZonedDateTime nextRetryAt;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    public enum RetryStatus {
        ATTEMPTED, SUCCESS, FAILED, SCHEDULED
    }
}
