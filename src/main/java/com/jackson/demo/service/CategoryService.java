package com.jackson.demo.service;

import com.jackson.demo.dto.request.CategoryRequest;
import com.jackson.demo.dto.response.CategoryResponse;
import com.jackson.demo.entity.Category;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.mapper.ApiMapper;
import com.jackson.demo.repository.CategoryRepository;
import com.jackson.demo.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(ApiMapper::toCategoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        return ApiMapper.toCategoryResponse(findCategory(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new BadRequestException("Category with this name already exists");
        });

        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return ApiMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        categoryRepository.findByNameIgnoreCase(request.name().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(category.getId())) {
                throw new BadRequestException("Category with this name already exists");
            }
        });
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return ApiMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        long productCount = productRepository.countByCategoryId(category.getId());
        if (productCount > 0) {
            throw new BadRequestException("Category cannot be deleted because it has products");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
