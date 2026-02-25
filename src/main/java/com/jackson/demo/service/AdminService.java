package com.jackson.demo.service;

import com.jackson.demo.dto.response.AdminUserResponse;
import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.entity.AppUser;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.repository.AppUserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final OrderService orderService;
    private final AppUserRepository appUserRepository;

    public AdminService(OrderService orderService, AppUserRepository appUserRepository) {
        this.orderService = orderService;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAllOrders() {
        return orderService.listAllOrders();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return appUserRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toAdminUserResponse).toList();
    }

    @SuppressWarnings("null")
    @Transactional
    public AdminUserResponse setUserEnabled(Long userId, boolean enabled) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getEmail().equalsIgnoreCase("admin@shop.local") && !enabled) {
            throw new BadRequestException("Default admin account cannot be disabled");
        }
        user.setEnabled(enabled);
        return toAdminUserResponse(appUserRepository.save(user));
    }

    private AdminUserResponse toAdminUserResponse(AppUser user) {
        Long customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
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
