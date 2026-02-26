package com.jackson.demo.config;

import com.jackson.demo.entity.AppUser;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.Category;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.PaymentMethod;
import com.jackson.demo.entity.Product;
import com.jackson.demo.model.PaymentProvider;
import com.jackson.demo.model.UserRole;
import com.jackson.demo.repository.AppUserRepository;
import com.jackson.demo.repository.CartItemRepository;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CategoryRepository;
import com.jackson.demo.repository.CustomerRepository;
import com.jackson.demo.repository.OrderItemRepository;
import com.jackson.demo.repository.PaymentMethodRepository;
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

    @SuppressWarnings("null")
    @Bean
    @Transactional
    ApplicationRunner seedData(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            OrderItemRepository orderItemRepository,
            AppUserRepository appUserRepository,
            CustomerRepository customerRepository,
            CartRepository cartRepository,
            PaymentMethodRepository paymentMethodRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {

            // ── CATEGORIES (seed if not exists) ──────────────────────────
            // NOTE: NO deleteAll calls here — Neon is a persistent DB, wiping
            // tables on every restart would delete all your data!
            Map<String, Category> categoriesByName = new HashMap<>();

            List<String[]> categoryDefs = List.of(
                    new String[]{"Shoes",                 "Sneakers, boots, loafers, sandals, and formal footwear"},
                    new String[]{"T-Shirts & Tops",       "Everyday tees, tanks, polos, and layering tops"},
                    new String[]{"Jeans & Trousers",      "Denim, chinos, joggers, and tailored pants"},
                    new String[]{"Jackets & Outerwear",   "Bomber, denim, puffer, trench, and winter jackets"},
                    new String[]{"Dresses & Skirts",      "Casual, workwear, party dresses, and skirts"},
                    new String[]{"Activewear",            "Gym leggings, sports bras, shorts, and training sets"},
                    new String[]{"Underwear & Loungewear","Comfort basics, sleepwear, and home essentials"},
                    new String[]{"Hoodies & Sweatshirts", "Pullovers, zip hoodies, and oversized sweatshirts"});

            for (String[] def : categoryDefs) {
                Category cat = categoryRepository.findByNameIgnoreCase(def[0]).orElseGet(() -> {
                    Category c = new Category();
                    c.setName(def[0]);
                    c.setDescription(def[1]);
                    return categoryRepository.save(c);
                });
                categoriesByName.put(cat.getName(), cat);
            }

            // ── PRODUCTS (seed if not exists) ─────────────────────────────
            List<Object[]> productDefs = List.of(
                    new Object[]{"Classic White Sneakers",
                            "Minimal everyday sneakers with cushioned insoles and durable rubber outsole",
                            "Shoes", new BigDecimal("74.99"), 110,
                            List.of("https://images.unsplash.com/photo-1549298916-b41d501d3772",
                                    "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77")},
                    new Object[]{"Leather Chelsea Boots",
                            "Sleek ankle-high leather boots with elastic side panels and pull tabs",
                            "Shoes", new BigDecimal("129.00"), 65,
                            List.of("https://images.unsplash.com/photo-1608256246200-53e635b5b65f",
                                    "https://images.unsplash.com/photo-1638247025967-b4e38f787b76")},
                    new Object[]{"Performance Running Shoes",
                            "Breathable running shoes with responsive foam and lightweight build",
                            "Shoes", new BigDecimal("99.90"), 95,
                            List.of("https://images.unsplash.com/photo-1543508282-6319a3e2621f",
                                    "https://images.unsplash.com/photo-1515955656352-a1fa3ffcd111")},
                    new Object[]{"Canvas Slip-On Shoes",
                            "Easy slip-on casual shoes with soft textile lining and flexible sole",
                            "Shoes", new BigDecimal("49.50"), 140,
                            List.of("https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77",
                                    "https://images.unsplash.com/photo-1460353581641-37baddab0fa2")},
                    new Object[]{"Nike Airfoce 1",
                            "Nike Airforce 1 shoes",
                            "Shoes", new BigDecimal("175.00"), 55,
                            List.of("https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?q=80&w=687&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                                    "https://images.unsplash.com/photo-1615109255391-b0dfdcd5b8d8?q=80&w=1170&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D")},
                    new Object[]{"Grasshopper",
                            "Grasshopper Hornsby Shoe",
                            "Shoes", new BigDecimal("65.00"), 89,
                            List.of("https://www.californian.co.za/wp-content/uploads/2024/04/A155-GRASSHOPPERS-BLACK-HORNSBY-TORNADO1.jpg",
                                    "https://johncraig.co.za/media/catalog/product/cache/8e71962534cb112d3996d77af375c1d8/g/r/gra8lbk-grasshopper-spoiler-leather-black-82-512500b-v1_jpg.jpg")},
                    new Object[]{"Oversized Graphic Tee",
                            "Relaxed cotton t-shirt with soft hand feel and bold front print",
                            "T-Shirts & Tops", new BigDecimal("29.99"), 180,
                            List.of("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab",
                                    "https://images.unsplash.com/photo-1583743814966-8936f37f4678")},
                    new Object[]{"Slim Fit Polo Shirt",
                            "Breathable pique polo with stretch and structured collar",
                            "T-Shirts & Tops", new BigDecimal("34.00"), 130,
                            List.of("https://images.unsplash.com/photo-1562157873-818bc0726f68",
                                    "https://images.unsplash.com/photo-1516826957135-700dedea698c")},
                    new Object[]{"Ribbed Crop Top",
                            "Stretch rib-knit crop top designed for layering or warm days",
                            "T-Shirts & Tops", new BigDecimal("22.90"), 160,
                            List.of("https://images.unsplash.com/photo-1485230895905-ec40ba36b9bc",
                                    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1")},
                    new Object[]{"Linen Button-Down Shirt",
                            "Lightweight linen long-sleeve shirt for smart-casual outfits",
                            "T-Shirts & Tops", new BigDecimal("46.75"), 85,
                            List.of("https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf",
                                    "https://images.unsplash.com/photo-1490114538077-0a7f8cb49891")},
                    new Object[]{"Straight-Leg Blue Jeans",
                            "Mid-rise denim jeans with classic straight silhouette",
                            "Jeans & Trousers", new BigDecimal("59.99"), 150,
                            List.of("https://images.unsplash.com/photo-1541099649105-f69ad21f3246",
                                    "https://images.unsplash.com/photo-1473966968600-fa801b869a1a")},
                    new Object[]{"Stretch Chino Pants",
                            "Comfort stretch chinos suitable for office and weekend wear",
                            "Jeans & Trousers", new BigDecimal("52.40"), 125,
                            List.of("https://images.unsplash.com/photo-1473966968600-fa801b869a1a",
                                    "https://images.unsplash.com/photo-1548883354-94bcfe321cbb")},
                    new Object[]{"Wide-Leg Trousers",
                            "Flowing high-waist trousers with front pleats and side pockets",
                            "Jeans & Trousers", new BigDecimal("64.20"), 70,
                            List.of("https://images.unsplash.com/photo-1489987707025-afc232f7ea0f",
                                    "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c")},
                    new Object[]{"Fleece Jogger Pants",
                            "Warm fleece joggers with tapered fit and adjustable waistband",
                            "Jeans & Trousers", new BigDecimal("39.95"), 155,
                            List.of("https://images.unsplash.com/photo-1516826957135-700dedea698c",
                                    "https://images.unsplash.com/photo-1503341504253-dff4815485f1")},
                    new Object[]{"Denim Jacket",
                            "Classic trucker-style denim jacket with functional chest pockets",
                            "Jackets & Outerwear", new BigDecimal("79.00"), 90,
                            List.of("https://images.unsplash.com/photo-1521223890158-f9f7c3d5d504",
                                    "https://images.unsplash.com/photo-1551028719-00167b16eac5")},
                    new Object[]{"Quilted Puffer Jacket",
                            "Insulated puffer jacket for cold weather with zip pockets",
                            "Jackets & Outerwear", new BigDecimal("119.99"), 75,
                            List.of("https://images.unsplash.com/photo-1544022613-e87ca75a784a",
                                    "https://images.unsplash.com/photo-1543076447-215ad9ba6923")},
                    new Object[]{"Water-Resistant Trench Coat",
                            "Longline trench coat with belt and storm flap detailing",
                            "Jackets & Outerwear", new BigDecimal("134.50"), 48,
                            List.of("https://images.unsplash.com/photo-1485968579580-b6d095142e6e",
                                    "https://images.unsplash.com/photo-1495385794356-15371f348c31")},
                    new Object[]{"Hooded Windbreaker",
                            "Lightweight windbreaker for travel and transitional weather",
                            "Jackets & Outerwear", new BigDecimal("68.80"), 102,
                            List.of("https://images.unsplash.com/photo-1483985988355-763728e1935b",
                                    "https://images.unsplash.com/photo-1485968579580-b6d095142e6e")},
                    new Object[]{"Floral Midi Dress",
                            "Flowy midi dress with floral print and waist tie",
                            "Dresses & Skirts", new BigDecimal("72.30"), 88,
                            List.of("https://images.unsplash.com/photo-1496747611176-843222e1e57c",
                                    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1")},
                    new Object[]{"Pleated Mini Skirt",
                            "Structured mini skirt with pleated finish and side zipper",
                            "Dresses & Skirts", new BigDecimal("41.60"), 118,
                            List.of("https://images.unsplash.com/photo-1521572267360-ee0c2909d518",
                                    "https://images.unsplash.com/photo-1529139574466-a303027c1d8b")},
                    new Object[]{"Satin Slip Dress",
                            "Soft satin slip dress for evening events and layering looks",
                            "Dresses & Skirts", new BigDecimal("89.99"), 52,
                            List.of("https://images.unsplash.com/photo-1464863979621-258859e62245",
                                    "https://images.unsplash.com/photo-1483985988355-763728e1935b")},
                    new Object[]{"Green sleeve dress",
                            "Green Sleeve Dress",
                            "Dresses & Skirts", new BigDecimal("75.00"), 95,
                            List.of("https://images.unsplash.com/flagged/photo-1585052201332-b8c0ce30972f?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8ZHJlc3N8ZW58MHx8MHx8fDA%3D")},
                    new Object[]{"Monique Lhuillier",
                            "Bow-detail silk midi dress",
                            "Dresses & Skirts", new BigDecimal("57.00"), 60,
                            List.of("https://www.mytheresa.com/media/1094/1238/100/9b/P00959181.jpg")},
                    new Object[]{"Athletic Compression Leggings",
                            "High-rise compression leggings with moisture-wicking fabric",
                            "Activewear", new BigDecimal("44.95"), 135,
                            List.of("https://images.unsplash.com/photo-1518611012118-696072aa579a",
                                    "https://images.unsplash.com/photo-1594737625785-a6cbdabd333c")},
                    new Object[]{"Training Shorts",
                            "Quick-dry training shorts with breathable mesh panels",
                            "Activewear", new BigDecimal("27.50"), 170,
                            List.of("https://images.unsplash.com/photo-1461896836934-ffe607ba8211",
                                    "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c")},
                    new Object[]{"Support Sports Bra",
                            "Medium-impact sports bra with removable pads and stretch support",
                            "Activewear", new BigDecimal("31.25"), 145,
                            List.of("https://images.unsplash.com/photo-1506629082955-511b1aa562c8",
                                    "https://images.unsplash.com/photo-1518611012118-696072aa579a")},
                    new Object[]{"Cotton Lounge Set",
                            "Two-piece breathable lounge set for home comfort",
                            "Underwear & Loungewear", new BigDecimal("54.40"), 98,
                            List.of("https://images.unsplash.com/photo-1512436991641-6745cdb1723f",
                                    "https://images.unsplash.com/photo-1503342217505-b0a15ec3261c")},
                    new Object[]{"Thermal Base Layer Top",
                            "Soft thermal top for layering in cold weather",
                            "Underwear & Loungewear", new BigDecimal("24.70"), 160,
                            List.of("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab",
                                    "https://images.unsplash.com/photo-1506629082955-511b1aa562c8")},
                    new Object[]{"Oversized Pullover Hoodie",
                            "Heavyweight fleece hoodie with front kangaroo pocket",
                            "Hoodies & Sweatshirts", new BigDecimal("48.90"), 175,
                            List.of("https://images.unsplash.com/photo-1556821840-3a9fbc6a4f79",
                                    "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab")},
                    new Object[]{"Zip-Up Sweatshirt",
                            "Full-zip sweatshirt with brushed interior and ribbed cuffs",
                            "Hoodies & Sweatshirts", new BigDecimal("45.30"), 142,
                            List.of("https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf",
                                    "https://images.unsplash.com/photo-1551028719-00167b16eac5")});

            for (Object[] def : productDefs) {
                String productName = (String) def[0];
                if (productRepository.findByNameIgnoreCase(productName).isEmpty()) {
                    Product product = new Product();
                    product.setName(productName);
                    product.setDescription((String) def[1]);
                    product.setCategory(categoriesByName.get((String) def[2]));
                    product.setPrice((BigDecimal) def[3]);
                    product.setStockQuantity((int) def[4]);
                    product.setActive(true);
                    @SuppressWarnings("unchecked")
                    List<String> urls = (List<String>) def[5];
                    product.setImageUrls(urls);
                    productRepository.save(product);
                }
            }

            // ── ADMIN USER (seed if not exists) ───────────────────────────
            appUserRepository.findByEmailIgnoreCase("admin@shop.local")
                    .map(existingAdmin -> {
                        boolean changed = existingAdmin.getRoles().add(UserRole.ROLE_ADMIN);
                        changed = existingAdmin.getRoles().add(UserRole.ROLE_CUSTOMER) || changed;
                        return changed ? appUserRepository.save(existingAdmin) : existingAdmin;
                    })
                    .orElseGet(() -> {
                        AppUser admin = new AppUser();
                        admin.setEmail("admin@shop.local");
                        admin.setFullName("System Admin");
                        admin.setPasswordHash(passwordEncoder.encode("Admin@12345"));
                        admin.getRoles().add(UserRole.ROLE_ADMIN);
                        admin.getRoles().add(UserRole.ROLE_CUSTOMER);
                        return appUserRepository.save(admin);
                    });

            // ── CUSTOMER USERS (seed if not exists) ───────────────────────
            List<Object[]> customerDefs = List.of(
                    new Object[]{"customer@shop.local", "Demo Customer",  "+1-202-555-0181", "742 Evergreen Terrace", "Customer@123"},
                    new Object[]{"alice@shop.local",    "Alice Johnson",  "+1-303-555-0102", "12 Maple Street",       "Alice@12345"},
                    new Object[]{"bob@shop.local",      "Bob Smith",      "+1-404-555-0193", "88 Oak Avenue",         "Bob@12345"},
                    new Object[]{"carol@shop.local",    "Carol Williams", "+1-505-555-0147", "5 Pine Road",           "Carol@12345"},
                    new Object[]{"david@shop.local",    "David Brown",    "+1-606-555-0168", "99 Birch Lane",         "David@12345"});

            for (Object[] cd : customerDefs) {
                String email    = (String) cd[0];
                String fullName = (String) cd[1];
                String phone    = (String) cd[2];
                String address  = (String) cd[3];
                String password = (String) cd[4];

                appUserRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
                    Customer customer = customerRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
                        Customer c = new Customer();
                        c.setFullName(fullName);
                        c.setEmail(email);
                        c.setPhone(phone);
                        c.setAddress(address);
                        return customerRepository.save(c);
                    });

                    cartRepository.findByCustomerId(customer.getId()).orElseGet(() -> {
                        Cart cart = new Cart();
                        cart.setCustomer(customer);
                        return cartRepository.save(cart);
                    });

                    AppUser user = new AppUser();
                    user.setEmail(email);
                    user.setFullName(fullName);
                    user.setPasswordHash(passwordEncoder.encode(password));
                    user.getRoles().add(UserRole.ROLE_CUSTOMER);
                    user.setCustomer(customer);
                    return appUserRepository.save(user);
                });

                customerRepository.findByEmailIgnoreCase(email).ifPresent(customer -> {
                    if (paymentMethodRepository.countByCustomerId(customer.getId()) == 0) {
                        PaymentMethod method = new PaymentMethod();
                        method.setCustomer(customer);
                        method.setProvider(PaymentProvider.CARD);
                        method.setCardHolderName(customer.getFullName());
                        method.setBrand("VISA");
                        method.setLast4("4242");
                        method.setExpiryMonth(12);
                        method.setExpiryYear(2030);
                        method.setBillingAddress(customer.getAddress());
                        method.setDefaultMethod(true);
                        method.setEnabled(true);
                        paymentMethodRepository.save(method);
                    }
                });
            }
        };
    }
}