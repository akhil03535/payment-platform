package com.paymentplatform.repository;

import com.paymentplatform.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionReference(String transactionReference);
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Transaction> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'CREDIT' AND t.status = 'COMPLETED'")
    java.math.BigDecimal sumCreditsByUserId(@Param("userId") UUID userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'DEBIT' AND t.status = 'COMPLETED'")
    java.math.BigDecimal sumDebitsByUserId(@Param("userId") UUID userId);
}
