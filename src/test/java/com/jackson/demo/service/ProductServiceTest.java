package com.jackson.demo.service;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackson.demo.dto.request.ProductRequest;
import com.jackson.demo.entity.Category;
import com.jackson.demo.entity.Product;
import com.jackson.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Electronics");
        CategoryService fakeCategoryService = new CategoryService(null, null) {
            @Override
            public Category findCategory(UUID id) {
                return category;
            }
        };
        productService = new ProductService(productRepository, fakeCategoryService);
    }

    @SuppressWarnings("null")
    @Test
    void createProductTrimsImageUrlsAndDefaultsActive() {
        ProductRequest request = new ProductRequest(
                "Camera",
                "4k",
                new BigDecimal("499.99"),
                8,
                UUID.randomUUID(),
                List.of(" https://img.one.jpg ", "https://img.two.jpg "),
                null);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.createProduct(request);

        assertEquals(List.of("https://img.one.jpg", "https://img.two.jpg"), response.imageUrls());
        assertTrue(response.active());
    }

    @Test
    void listProductsWithQueryUsesSearchRepositoryMethod() {
        Category category = new Category();
        category.setName("Electronics");
        Product product = new Product();
        product.setName("Action Camera");
        product.setDescription("desc");
        product.setPrice(new BigDecimal("149.00"));
        product.setStockQuantity(10);
        product.setActive(true);
        product.setImageUrls(List.of("https://img.one.jpg"));
        product.setCategory(category);

        when(productRepository.findByNameContainingIgnoreCase("cam")).thenReturn(List.of(product));

        var result = productService.listProducts(" cam ");

        assertEquals(1, result.size());
        verify(productRepository).findByNameContainingIgnoreCase("cam");
        verify(productRepository, never()).findAll();
    }
}
