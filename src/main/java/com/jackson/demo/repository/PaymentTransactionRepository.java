package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.PaymentTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByOrderIdOrderByProcessedAtDesc(UUID orderId);
    List<PaymentTransaction> findByCustomerIdOrderByProcessedAtDesc(UUID customerId);
}
