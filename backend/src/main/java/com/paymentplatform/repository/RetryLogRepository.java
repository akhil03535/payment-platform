package com.paymentplatform.repository;

import com.paymentplatform.entity.RetryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RetryLogRepository extends JpaRepository<RetryLog, UUID> {
    List<RetryLog> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
    long countByPaymentId(UUID paymentId);
}
