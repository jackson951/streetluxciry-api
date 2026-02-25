package com.jackson.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackson.demo.dto.request.CustomerRequest;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CustomerService customerService;

    @SuppressWarnings("null")
    @Test
    void createCustomerThrowsWhenEmailAlreadyExists() {
        CustomerRequest request = new CustomerRequest("John Doe", "john@shop.local", null, null);
        when(customerRepository.findByEmailIgnoreCase("john@shop.local")).thenReturn(Optional.of(new Customer()));

        assertThrows(BadRequestException.class, () -> customerService.createCustomer(request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @SuppressWarnings("null")
    @Test
    void updateCustomerThrowsWhenEmailBelongsToAnotherCustomer() {
        Customer current = new Customer();
        ReflectionTestUtils.setField(current, "id", 1L);
        current.setEmail("current@shop.local");

        Customer other = new Customer();
        ReflectionTestUtils.setField(other, "id", 2L);
        other.setEmail("existing@shop.local");

        CustomerRequest request = new CustomerRequest("Current", "existing@shop.local", "123", "Addr");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(customerRepository.findByEmailIgnoreCase("existing@shop.local")).thenReturn(Optional.of(other));

        assertThrows(BadRequestException.class, () -> customerService.updateCustomer(1L, request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @SuppressWarnings("null")
    @Test
    void updateCustomerAllowsSameOwnerEmailAndNormalizesEmail() {
        Customer current = new Customer();
        ReflectionTestUtils.setField(current, "id", 1L);
        current.setEmail("same@shop.local");

        CustomerRequest request = new CustomerRequest("Jane Doe", " SAME@SHOP.LOCAL ", "555", "Earth");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(customerRepository.findByEmailIgnoreCase("same@shop.local")).thenReturn(Optional.of(current));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerService.updateCustomer(1L, request);

        assertEquals("same@shop.local", response.email());
        assertEquals("Jane Doe", response.fullName());
    }
}
