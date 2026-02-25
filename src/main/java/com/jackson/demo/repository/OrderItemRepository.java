package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
