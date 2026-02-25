package com.jackson.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackson.demo.dto.request.AdminUserUpdateRequest;
import com.jackson.demo.entity.AppUser;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.model.UserRole;
import com.jackson.demo.repository.AppUserRepository;
import com.jackson.demo.repository.CustomerRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void updateUserUpdatesAllEditableFields() {
        AdminService adminService = new AdminService(null, appUserRepository, customerRepository, passwordEncoder);

        Customer customer = new Customer();
        ReflectionTestUtils.setField(customer, "id", 21L);
        customer.setFullName("Old Name");
        customer.setEmail("old@shop.local");
        customer.setPhone("111");
        customer.setAddress("Old");

        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", 11L);
        user.setEmail("old@shop.local");
        user.setFullName("Old Name");
        user.setPasswordHash("old-hash");
        user.setEnabled(true);
        user.getRoles().add(UserRole.ROLE_CUSTOMER);
        user.setCustomer(customer);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest(
                " New@Shop.Local ",
                " New Name ",
                "NewPassword123",
                Set.of(UserRole.ROLE_ADMIN, UserRole.ROLE_CUSTOMER),
                true,
                "222",
                "New Address");

        when(appUserRepository.findById(11L)).thenReturn(Optional.of(user));
        when(appUserRepository.findByEmailIgnoreCase("new@shop.local")).thenReturn(Optional.of(user));
        when(customerRepository.findByEmailIgnoreCase("new@shop.local")).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = adminService.updateUser(11L, request);

        assertEquals("new@shop.local", response.email());
        assertEquals("New Name", response.fullName());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_CUSTOMER"), response.roles());
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals("new@shop.local", customer.getEmail());
        assertEquals("New Name", customer.getFullName());
        assertEquals("222", customer.getPhone());
        assertEquals("New Address", customer.getAddress());
        verify(customerRepository).save(customer);
        verify(appUserRepository).save(user);
    }

    @Test
    void updateUserRejectsRemovingAdminRoleFromDefaultAdmin() {
        AdminService adminService = new AdminService(null, appUserRepository, customerRepository, passwordEncoder);

        AppUser admin = new AppUser();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setEmail("admin@shop.local");
        admin.setFullName("Admin");
        admin.setPasswordHash("hash");
        admin.setEnabled(true);
        admin.getRoles().add(UserRole.ROLE_ADMIN);

        AdminUserUpdateRequest request = new AdminUserUpdateRequest(
                "admin@shop.local",
                "Admin",
                "Admin@12345",
                Set.of(UserRole.ROLE_CUSTOMER),
                true,
                null,
                null);

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(appUserRepository.findByEmailIgnoreCase("admin@shop.local")).thenReturn(Optional.of(admin));
        when(customerRepository.findByEmailIgnoreCase("admin@shop.local")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> adminService.updateUser(1L, request));
    }
}
