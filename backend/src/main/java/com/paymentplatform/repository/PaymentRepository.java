package com.paymentplatform.repository;

import com.paymentplatform.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status IN ('FAILED', 'TIMEOUT') AND p.retryCount < p.maxRetries")
    List<Payment> findRetryablePayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.user.id = :userId AND p.status = :status")
    long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :start AND :end ORDER BY p.createdAt DESC")
    List<Payment> findByDateRange(@Param("start") ZonedDateTime start, @Param("end") ZonedDateTime end);

    @Query("SELECT p.status, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.user.id = :userId GROUP BY p.status")
    List<Object[]> getPaymentStatsByUser(@Param("userId") UUID userId);

    @Query("SELECT p.status, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.createdAt >= :since GROUP BY p.status")
    List<Object[]> getPaymentStatsSince(@Param("since") ZonedDateTime since);

    @Query(value = """
        SELECT DATE_TRUNC('day', created_at) as day, COUNT(*) as total,
               SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success,
               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
               SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as volume
        FROM payments
        WHERE created_at >= :since AND user_id = :userId
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> getDailyStatsByUser(@Param("userId") UUID userId, @Param("since") ZonedDateTime since);

    @Query(value = """
        SELECT DATE_TRUNC('day', created_at) as day, COUNT(*) as total,
               SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success,
               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed,
               SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as volume
        FROM payments
        WHERE created_at >= :since
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> getDailyStats(@Param("since") ZonedDateTime since);
}
