package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByNameContainingIgnoreCase(String keyword);
    long countByCategoryId(UUID categoryId);
}
