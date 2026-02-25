package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
