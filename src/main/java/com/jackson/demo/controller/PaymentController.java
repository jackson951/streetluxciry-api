package com.jackson.demo.controller;
import java.util.UUID;

import com.jackson.demo.dto.request.CreatePaymentMethodRequest;
import com.jackson.demo.dto.request.ProcessPaymentRequest;
import com.jackson.demo.dto.response.PaymentMethodResponse;
import com.jackson.demo.dto.response.PaymentTransactionResponse;
import com.jackson.demo.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Save a payment method for a customer")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @PostMapping("/customers/{customerId}/payment-methods")
    public ResponseEntity<PaymentMethodResponse> createPaymentMethod(
            @PathVariable UUID customerId,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPaymentMethod(customerId, request));
    }

    @Operation(summary = "List customer payment methods")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @GetMapping("/customers/{customerId}/payment-methods")
    public List<PaymentMethodResponse> listCustomerPaymentMethods(@PathVariable UUID customerId) {
        return paymentService.listCustomerPaymentMethods(customerId);
    }

    @Operation(summary = "Set default payment method")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @PatchMapping("/customers/{customerId}/payment-methods/{paymentMethodId}/default")
    public PaymentMethodResponse setDefaultPaymentMethod(
            @PathVariable UUID customerId,
            @PathVariable UUID paymentMethodId) {
        return paymentService.setDefaultPaymentMethod(customerId, paymentMethodId);
    }

    @Operation(summary = "Enable or disable a payment method")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @PatchMapping("/customers/{customerId}/payment-methods/{paymentMethodId}/access")
    public PaymentMethodResponse setPaymentMethodEnabled(
            @PathVariable UUID customerId,
            @PathVariable UUID paymentMethodId,
            @RequestParam boolean enabled) {
        return paymentService.setPaymentMethodEnabled(customerId, paymentMethodId, enabled);
    }

    @Operation(summary = "Process payment for an order (approved/declined)")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessOrder(#orderId, authentication)")
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PaymentTransactionResponse> processPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processOrderPayment(orderId, request));
    }

    @Operation(summary = "List payment attempts for an order")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessOrder(#orderId, authentication)")
    @GetMapping("/orders/{orderId}/payments")
    public List<PaymentTransactionResponse> listOrderPayments(@PathVariable UUID orderId) {
        return paymentService.listOrderPayments(orderId);
    }

    @Operation(summary = "List all payments for a customer")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @GetMapping("/customers/{customerId}/payments")
    public List<PaymentTransactionResponse> listCustomerPayments(@PathVariable UUID customerId) {
        return paymentService.listCustomerPayments(customerId);
    }
}
