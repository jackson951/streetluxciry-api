package com.jackson.demo.service;
import java.util.UUID;

import com.jackson.demo.dto.request.ProductRequest;
import com.jackson.demo.dto.response.ProductResponse;
import com.jackson.demo.entity.Product;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.mapper.ApiMapper;
import com.jackson.demo.repository.ProductRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    @Cacheable(cacheNames = "productLists", key = "#q == null ? '' : #q.trim().toLowerCase()")
    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(String q) {
        List<Product> products = (q == null || q.isBlank())
                ? productRepository.findAll()
                : productRepository.findByNameContainingIgnoreCase(q.trim());
        return products.stream().map(ApiMapper::toProductResponse).toList();
    }

    @Cacheable(cacheNames = "productById", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return ApiMapper.toProductResponse(findProduct(id));
    }

    @CacheEvict(cacheNames = "productLists", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return ApiMapper.toProductResponse(productRepository.save(product));
    }

    @SuppressWarnings("null")
    @Caching(evict = {
        @CacheEvict(cacheNames = "productById", key = "#id"),
        @CacheEvict(cacheNames = "productLists", allEntries = true)
    })
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = findProduct(id);
        applyRequest(product, request);
        return ApiMapper.toProductResponse(productRepository.save(product));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "productById", key = "#id"),
        @CacheEvict(cacheNames = "productLists", allEntries = true)
    })
    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.delete(findProduct(id));
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public Product findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(categoryService.findCategory(request.categoryId()));
        product.setImageUrls(request.imageUrls().stream().map(String::trim).toList());
        product.setActive(request.active() == null || request.active());
    }
}
