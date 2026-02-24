package com.jackson.demo.config;

import com.jackson.demo.entity.AppUser;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.Category;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.Product;
import com.jackson.demo.model.UserRole;
import com.jackson.demo.repository.AppUserRepository;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CategoryRepository;
import com.jackson.demo.repository.CustomerRepository;
import com.jackson.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataSeeder {

    @Bean
    @Transactional
    ApplicationRunner seedData(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            AppUserRepository appUserRepository,
            CustomerRepository customerRepository,
            CartRepository cartRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Map<String, Category> categoriesByName = new HashMap<>();

            if (categoryRepository.count() == 0) {
                List<Category> categories = List.of(
                        createCategory("Electronics", "Phones, laptops, audio, and gadgets"),
                        createCategory("Fashion", "Clothing, footwear, and accessories"),
                        createCategory("Home & Kitchen", "Home essentials and kitchen appliances"),
                        createCategory("Beauty", "Skincare, haircare, and cosmetics"),
                        createCategory("Sports", "Fitness and outdoor equipment"));

                categoryRepository.saveAll(categories);
            }

            categoryRepository.findAll().forEach(category -> categoriesByName.put(category.getName(), category));

            if (productRepository.count() == 0) {
                List<Product> products = List.of(
                        createProduct(
                                "Wireless Noise-Cancelling Headphones",
                                "Premium over-ear Bluetooth headphones with ANC and 30-hour battery life",
                                "Electronics",
                                new BigDecimal("199.99"),
                                45,
                                List.of(
                                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e",
                                        "https://images.unsplash.com/photo-1583394838336-acd977736f90"),
                                categoriesByName),
                        createProduct(
                                "4K Action Camera",
                                "Waterproof action camera with image stabilization and Wi-Fi control",
                                "Electronics",
                                new BigDecimal("149.00"),
                                60,
                                List.of(
                                        "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f",
                                        "https://images.unsplash.com/photo-1516035069371-29a1b244cc32"),
                                categoriesByName),
                        createProduct(
                                "Men's Running Sneakers",
                                "Lightweight breathable sneakers designed for road and gym workouts",
                                "Fashion",
                                new BigDecimal("89.50"),
                                120,
                                List.of(
                                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff",
                                        "https://images.unsplash.com/photo-1460353581641-37baddab0fa2"),
                                categoriesByName),
                        createProduct(
                                "Stainless Steel Cookware Set",
                                "10-piece induction-compatible cookware set with tempered glass lids",
                                "Home & Kitchen",
                                new BigDecimal("129.99"),
                                35,
                                List.of(
                                        "https://images.unsplash.com/photo-1584990347449-a52f7f0b7731",
                                        "https://images.unsplash.com/photo-1528715471579-d1bcf0ba5e83"),
                                categoriesByName),
                        createProduct(
                                "Vitamin C Face Serum",
                                "Daily brightening serum with hyaluronic acid and vitamin C",
                                "Beauty",
                                new BigDecimal("24.99"),
                                200,
                                List.of(
                                        "https://images.unsplash.com/photo-1571781926291-c477ebfd024b",
                                        "https://images.unsplash.com/photo-1612817288484-6f916006741a"),
                                categoriesByName),
                        createProduct(
                                "Adjustable Dumbbell Set",
                                "Pair of adjustable dumbbells for home strength training",
                                "Sports",
                                new BigDecimal("219.00"),
                                25,
                                List.of(
                                        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438",
                                        "https://images.unsplash.com/photo-1599058917212-d750089bc07e"),
                                categoriesByName));

                productRepository.saveAll(products);
            }

            appUserRepository.findByEmailIgnoreCase("admin@shop.local").orElseGet(() -> {
                AppUser admin = new AppUser();
                admin.setEmail("admin@shop.local");
                admin.setFullName("System Admin");
                admin.setPasswordHash(passwordEncoder.encode("Admin@12345"));
                admin.getRoles().add(UserRole.ROLE_ADMIN);
                return appUserRepository.save(admin);
            });

            appUserRepository.findByEmailIgnoreCase("customer@shop.local").orElseGet(() -> {
                Customer customer = customerRepository.findByEmailIgnoreCase("customer@shop.local").orElseGet(() -> {
                    Customer created = new Customer();
                    created.setFullName("Demo Customer");
                    created.setEmail("customer@shop.local");
                    created.setPhone("+1-202-555-0181");
                    created.setAddress("742 Evergreen Terrace");
                    return customerRepository.save(created);
                });
                cartRepository.findByCustomerId(customer.getId()).orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });

                AppUser shopper = new AppUser();
                shopper.setEmail("customer@shop.local");
                shopper.setFullName("Demo Customer");
                shopper.setPasswordHash(passwordEncoder.encode("Customer@123"));
                shopper.getRoles().add(UserRole.ROLE_CUSTOMER);
                shopper.setCustomer(customer);
                return appUserRepository.save(shopper);
            });
        };
    }

    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    private Product createProduct(
            String name,
            String description,
            String categoryName,
            BigDecimal price,
            int stockQuantity,
            List<String> imageUrls,
            Map<String, Category> categoriesByName) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setActive(true);
        product.setImageUrls(imageUrls);
        product.setCategory(categoriesByName.get(categoryName));
        return product;
    }
}
