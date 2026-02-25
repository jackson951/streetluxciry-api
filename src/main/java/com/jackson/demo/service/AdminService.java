package com.jackson.demo.service;
import java.util.UUID;

import com.jackson.demo.dto.request.AdminUserUpdateRequest;
import com.jackson.demo.dto.response.OrderTrackingResponse;
import com.jackson.demo.dto.response.AdminUserResponse;
import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.model.OrderStatus;
import com.jackson.demo.entity.AppUser;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.model.UserRole;
import com.jackson.demo.repository.AppUserRepository;
import com.jackson.demo.repository.CustomerRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final OrderService orderService;
    private final AppUserRepository appUserRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            OrderService orderService,
            AppUserRepository appUserRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder) {
        this.orderService = orderService;
        this.appUserRepository = appUserRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAllOrders() {
        return orderService.listAllOrders();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus status) {
        return orderService.updateOrderStatus(orderId, status);
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(UUID orderId) {
        return orderService.getOrderTracking(orderId);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return appUserRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toAdminUserResponse).toList();
    }

    @SuppressWarnings("null")
    @Transactional
    public AdminUserResponse setUserEnabled(UUID userId, boolean enabled) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getEmail().equalsIgnoreCase("admin@shop.local") && !enabled) {
            throw new BadRequestException("Default admin account cannot be disabled");
        }
        user.setEnabled(enabled);
        return toAdminUserResponse(appUserRepository.save(user));
    }

    @SuppressWarnings("null")
    @Transactional
    public AdminUserResponse updateUser(UUID userId, AdminUserUpdateRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String normalizedEmail = request.email().trim().toLowerCase();
        appUserRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new BadRequestException("User with this email already exists");
            }
        });

        customerRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existingCustomer -> {
            if (user.getCustomer() == null || !existingCustomer.getId().equals(user.getCustomer().getId())) {
                throw new BadRequestException("Customer with this email already exists");
            }
        });

        if (user.getEmail().equalsIgnoreCase("admin@shop.local")) {
            if (!request.enabled()) {
                throw new BadRequestException("Default admin account cannot be disabled");
            }
            if (!request.roles().contains(UserRole.ROLE_ADMIN)) {
                throw new BadRequestException("Default admin account must keep admin role");
            }
        }

        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled());
        user.getRoles().clear();
        user.getRoles().addAll(request.roles());

        Customer customer = user.getCustomer();
        if (customer != null) {
            customer.setFullName(request.fullName().trim());
            customer.setEmail(normalizedEmail);
            customer.setPhone(request.phone());
            customer.setAddress(request.address());
            customerRepository.save(customer);
        }

        return toAdminUserResponse(appUserRepository.save(user));
    }

    private AdminUserResponse toAdminUserResponse(AppUser user) {
        UUID customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEnabled(),
                customerId,
                user.getCreatedAt());
    }
}
