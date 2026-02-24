package com.jackson.demo.repository;

import com.jackson.demo.entity.PaymentTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByOrderIdOrderByProcessedAtDesc(Long orderId);
    List<PaymentTransaction> findByCustomerIdOrderByProcessedAtDesc(Long customerId);
}
