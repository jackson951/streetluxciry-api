package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    Optional<PaymentMethod> findByIdAndCustomerId(UUID id, UUID customerId);
    long countByCustomerId(UUID customerId);
    Optional<PaymentMethod> findByCustomerIdAndDefaultMethodTrue(UUID customerId);
}
