package com.jackson.demo.repository;

import com.jackson.demo.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<PaymentMethod> findByIdAndCustomerId(Long id, Long customerId);
    long countByCustomerId(Long customerId);
    Optional<PaymentMethod> findByCustomerIdAndDefaultMethodTrue(Long customerId);
}
