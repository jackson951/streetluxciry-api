package com.jackson.demo.controller;

import com.jackson.demo.dto.request.AdminUserUpdateRequest;
import com.jackson.demo.dto.response.AdminUserResponse;
import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "List orders from all customers")
    @GetMapping("/orders")
    public List<OrderResponse> listAllOrders() {
        return adminService.listAllOrders();
    }

    @Operation(summary = "List all users")
    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @Operation(summary = "Enable or disable user access")
    @PatchMapping("/users/{userId}/access")
    public AdminUserResponse setUserAccess(@PathVariable Long userId, @RequestParam boolean enabled) {
        return adminService.setUserEnabled(userId, enabled);
    }

    @Operation(summary = "Update user details")
    @PutMapping("/users/{userId}")
    public AdminUserResponse updateUser(@PathVariable Long userId, @Valid @RequestBody AdminUserUpdateRequest request) {
        return adminService.updateUser(userId, request);
    }
}
