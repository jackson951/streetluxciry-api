package com.jackson.demo.security;

import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.repository.CustomerOrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("accessControlService")
public class AccessControlService {

    private final CustomerOrderRepository customerOrderRepository;

    public AccessControlService(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    public boolean canAccessCustomer(Long customerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (isAdmin(authentication)) {
            return true;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user.getCustomerId() != null && user.getCustomerId().equals(customerId);
        }
        return false;
    }

    @SuppressWarnings("null")
    public boolean canAccessOrder(Long orderId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (isAdmin(authentication)) {
            return true;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user) || user.getCustomerId() == null) {
            return false;
        }
        return customerOrderRepository.findById(orderId)
                .map(CustomerOrder::getCustomer)
                .map(customer -> customer.getId().equals(user.getCustomerId()))
                .orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
