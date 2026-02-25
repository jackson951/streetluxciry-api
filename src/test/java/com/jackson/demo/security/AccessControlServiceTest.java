package com.jackson.demo.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.repository.CustomerOrderRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Test
    void canAccessCustomerReturnsTrueForAdmin() {
        AccessControlService service = new AccessControlService(customerOrderRepository);
        UUID customerId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertTrue(service.canAccessCustomer(customerId, auth));
    }

    @Test
    void canAccessCustomerReturnsTrueForOwnerAndFalseForOther() {
        AccessControlService service = new AccessControlService(customerOrderRepository);
        UUID userId = UUID.randomUUID();
        UUID ownerCustomerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser(
                userId,
                ownerCustomerId,
                "user@shop.local",
                "x",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertTrue(service.canAccessCustomer(ownerCustomerId, auth));
        assertFalse(service.canAccessCustomer(otherCustomerId, auth));
    }

    @Test
    void canAccessOrderReturnsTrueOnlyForOwnerOrAdmin() {
        AccessControlService service = new AccessControlService(customerOrderRepository);
        UUID ownerCustomerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Customer owner = new Customer();
        ReflectionTestUtils.setField(owner, "id", ownerCustomerId);
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(owner);
        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        AuthenticatedUser ownerPrincipal = new AuthenticatedUser(
                UUID.randomUUID(),
                ownerCustomerId,
                "owner@shop.local",
                "x",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Authentication ownerAuth =
                new UsernamePasswordAuthenticationToken(ownerPrincipal, null, ownerPrincipal.getAuthorities());

        AuthenticatedUser otherPrincipal = new AuthenticatedUser(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "other@shop.local",
                "x",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Authentication otherAuth =
                new UsernamePasswordAuthenticationToken(otherPrincipal, null, otherPrincipal.getAuthorities());

        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertTrue(service.canAccessOrder(orderId, ownerAuth));
        assertFalse(service.canAccessOrder(orderId, otherAuth));
        assertTrue(service.canAccessOrder(orderId, adminAuth));
    }
}
