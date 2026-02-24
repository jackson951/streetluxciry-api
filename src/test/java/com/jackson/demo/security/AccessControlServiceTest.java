package com.jackson.demo.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.repository.CustomerOrderRepository;
import java.util.List;
import java.util.Optional;
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
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertTrue(service.canAccessCustomer(99L, auth));
    }

    @Test
    void canAccessCustomerReturnsTrueForOwnerAndFalseForOther() {
        AccessControlService service = new AccessControlService(customerOrderRepository);
        AuthenticatedUser principal = new AuthenticatedUser(
                10L,
                7L,
                "user@shop.local",
                "x",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertTrue(service.canAccessCustomer(7L, auth));
        assertFalse(service.canAccessCustomer(8L, auth));
    }

    @Test
    void canAccessOrderReturnsTrueOnlyForOwnerOrAdmin() {
        AccessControlService service = new AccessControlService(customerOrderRepository);

        Customer owner = new Customer();
        ReflectionTestUtils.setField(owner, "id", 7L);
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(owner);
        when(customerOrderRepository.findById(15L)).thenReturn(Optional.of(order));

        AuthenticatedUser ownerPrincipal = new AuthenticatedUser(
                10L,
                7L,
                "owner@shop.local",
                "x",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Authentication ownerAuth =
                new UsernamePasswordAuthenticationToken(ownerPrincipal, null, ownerPrincipal.getAuthorities());

        AuthenticatedUser otherPrincipal = new AuthenticatedUser(
                11L,
                8L,
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

        assertTrue(service.canAccessOrder(15L, ownerAuth));
        assertFalse(service.canAccessOrder(15L, otherAuth));
        assertTrue(service.canAccessOrder(15L, adminAuth));
    }
}
