package com.jackson.demo.repository;

import com.jackson.demo.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String keyword);
    long countByCategoryId(Long categoryId);
}
