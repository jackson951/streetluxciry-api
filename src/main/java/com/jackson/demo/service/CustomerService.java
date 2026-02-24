package com.jackson.demo.service;

import com.jackson.demo.dto.request.CustomerRequest;
import com.jackson.demo.dto.response.CustomerResponse;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.mapper.ApiMapper;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;

    public CustomerService(CustomerRepository customerRepository, CartRepository cartRepository) {
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers() {
        return customerRepository.findAll().stream().map(ApiMapper::toCustomerResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        return ApiMapper.toCustomerResponse(findCustomer(id));
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        customerRepository.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            throw new BadRequestException("Customer with this email already exists");
        });

        Customer customer = new Customer();
        applyRequest(customer, request);
        Customer saved = customerRepository.save(customer);

        Cart cart = new Cart();
        cart.setCustomer(saved);
        cartRepository.save(cart);

        return ApiMapper.toCustomerResponse(saved);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        String normalizedEmail = request.email().trim().toLowerCase();
        customerRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(customer.getId())) {
                throw new BadRequestException("Customer with this email already exists");
            }
        });
        applyRequest(customer, request);
        return ApiMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        customerRepository.delete(findCustomer(id));
    }

    @Transactional(readOnly = true)
    public Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setFullName(request.fullName().trim());
        customer.setEmail(request.email().trim().toLowerCase());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
    }
}
