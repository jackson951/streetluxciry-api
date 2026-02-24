package com.jackson.demo.service;

import com.jackson.demo.dto.request.CreatePaymentMethodRequest;
import com.jackson.demo.dto.request.ProcessPaymentRequest;
import com.jackson.demo.dto.response.PaymentMethodResponse;
import com.jackson.demo.dto.response.PaymentTransactionResponse;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.entity.PaymentMethod;
import com.jackson.demo.entity.PaymentTransaction;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.model.OrderStatus;
import com.jackson.demo.model.PaymentProvider;
import com.jackson.demo.model.PaymentStatus;
import com.jackson.demo.repository.CustomerOrderRepository;
import com.jackson.demo.repository.PaymentMethodRepository;
import com.jackson.demo.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final CustomerService customerService;
    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentService(
            CustomerService customerService,
            CustomerOrderRepository customerOrderRepository,
            PaymentMethodRepository paymentMethodRepository,
            PaymentTransactionRepository paymentTransactionRepository) {
        this.customerService = customerService;
        this.customerOrderRepository = customerOrderRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public PaymentMethodResponse createPaymentMethod(Long customerId, CreatePaymentMethodRequest request) {
        Customer customer = customerService.findCustomer(customerId);
        String normalizedCardNumber = normalizeCardNumber(request.cardNumber());
        validateExpiry(request.expiryMonth(), request.expiryYear());

        boolean setAsDefault = Boolean.TRUE.equals(request.defaultMethod()) ||
                paymentMethodRepository.countByCustomerId(customerId) == 0;
        if (setAsDefault) {
            clearDefaultMethod(customerId);
        }

        PaymentMethod method = new PaymentMethod();
        method.setCustomer(customer);
        method.setProvider(request.provider() == null ? PaymentProvider.CARD : request.provider());
        method.setCardHolderName(request.cardHolderName().trim());
        method.setBrand(resolveBrand(normalizedCardNumber, request.brand()));
        method.setLast4(last4(normalizedCardNumber));
        method.setExpiryMonth(request.expiryMonth());
        method.setExpiryYear(request.expiryYear());
        method.setBillingAddress(request.billingAddress());
        method.setDefaultMethod(setAsDefault);
        method.setEnabled(true);

        return toPaymentMethodResponse(paymentMethodRepository.save(method));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listCustomerPaymentMethods(Long customerId) {
        customerService.findCustomer(customerId);
        return paymentMethodRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toPaymentMethodResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse setDefaultPaymentMethod(Long customerId, Long paymentMethodId) {
        customerService.findCustomer(customerId);
        PaymentMethod method = findPaymentMethod(customerId, paymentMethodId);
        if (!method.isEnabled()) {
            throw new BadRequestException("Cannot set a disabled payment method as default");
        }
        clearDefaultMethod(customerId);
        method.setDefaultMethod(true);
        return toPaymentMethodResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public PaymentMethodResponse setPaymentMethodEnabled(Long customerId, Long paymentMethodId, boolean enabled) {
        customerService.findCustomer(customerId);
        PaymentMethod method = findPaymentMethod(customerId, paymentMethodId);
        method.setEnabled(enabled);
        if (!enabled && method.isDefaultMethod()) {
            method.setDefaultMethod(false);
            paymentMethodRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                    .filter(candidate -> !candidate.getId().equals(method.getId()) && candidate.isEnabled())
                    .findFirst()
                    .ifPresent(nextDefault -> nextDefault.setDefaultMethod(true));
        }
        return toPaymentMethodResponse(method);
    }

    @Transactional
    public PaymentTransactionResponse processOrderPayment(Long orderId, ProcessPaymentRequest request) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        validateOrderIsPayable(order);

        PaymentMethod method = paymentMethodRepository.findById(request.paymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + request.paymentMethodId()));

        if (!method.getCustomer().getId().equals(order.getCustomer().getId())) {
            throw new BadRequestException("Payment method does not belong to this order's customer");
        }
        if (!method.isEnabled()) {
            return saveDeclined(order, method, "METHOD_DISABLED", "Payment method is disabled");
        }
        if (isExpired(method.getExpiryMonth(), method.getExpiryYear())) {
            return saveDeclined(order, method, "EXPIRED_CARD", "Payment method is expired");
        }
        if (request.cvv() == null || !request.cvv().matches("^[0-9]{3,4}$")) {
            return saveDeclined(order, method, "INVALID_CVV", "Invalid CVV");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order total is invalid for payment");
        }

        boolean approved = order.getTotalAmount().compareTo(new BigDecimal("5000.00")) <= 0
                && !method.getLast4().equals("0000");
        if (!approved) {
            return saveDeclined(order, method, "DECLINED", "Payment declined by issuer");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setCustomer(order.getCustomer());
        transaction.setPaymentMethod(method);
        transaction.setStatus(PaymentStatus.APPROVED);
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrency("USD");
        transaction.setGatewayResponseCode("APPROVED");
        transaction.setGatewayMessage("Payment approved");

        order.setStatus(OrderStatus.PAID);
        customerOrderRepository.save(order);

        return toPaymentTransactionResponse(paymentTransactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> listOrderPayments(Long orderId) {
        customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return paymentTransactionRepository.findByOrderIdOrderByProcessedAtDesc(orderId).stream()
                .map(this::toPaymentTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> listCustomerPayments(Long customerId) {
        customerService.findCustomer(customerId);
        return paymentTransactionRepository.findByCustomerIdOrderByProcessedAtDesc(customerId).stream()
                .map(this::toPaymentTransactionResponse)
                .toList();
    }

    private PaymentMethod findPaymentMethod(Long customerId, Long paymentMethodId) {
        return paymentMethodRepository.findByIdAndCustomerId(paymentMethodId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + paymentMethodId));
    }

    private void clearDefaultMethod(Long customerId) {
        paymentMethodRepository.findByCustomerIdAndDefaultMethodTrue(customerId)
                .ifPresent(existing -> existing.setDefaultMethod(false));
    }

    private void validateOrderIsPayable(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled orders cannot be paid");
        }
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PROCESSING
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order is already paid");
        }
    }

    private PaymentTransactionResponse saveDeclined(
            CustomerOrder order, PaymentMethod method, String code, String message) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setCustomer(order.getCustomer());
        transaction.setPaymentMethod(method);
        transaction.setStatus(PaymentStatus.DECLINED);
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrency("USD");
        transaction.setGatewayResponseCode(code);
        transaction.setGatewayMessage(message);
        return toPaymentTransactionResponse(paymentTransactionRepository.save(transaction));
    }

    private String normalizeCardNumber(String cardNumber) {
        String normalized = cardNumber.replace(" ", "").trim();
        if (!normalized.matches("^[0-9]{12,19}$")) {
            throw new BadRequestException("Card number is invalid");
        }
        return normalized;
    }

    private String last4(String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    private String resolveBrand(String cardNumber, String customBrand) {
        if (customBrand != null && !customBrand.isBlank()) {
            return customBrand.trim();
        }
        if (cardNumber.startsWith("4")) {
            return "VISA";
        }
        if (cardNumber.startsWith("5")) {
            return "MASTERCARD";
        }
        if (cardNumber.startsWith("3")) {
            return "AMEX";
        }
        return "CARD";
    }

    private void validateExpiry(Integer month, Integer year) {
        if (isExpired(month, year)) {
            throw new BadRequestException("Expiry date cannot be in the past");
        }
    }

    private boolean isExpired(Integer month, Integer year) {
        YearMonth expiry = YearMonth.of(year, month);
        return expiry.isBefore(YearMonth.now());
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getId(),
                method.getProvider(),
                method.getCardHolderName(),
                method.getBrand(),
                method.getLast4(),
                method.getExpiryMonth(),
                method.getExpiryYear(),
                method.getBillingAddress(),
                method.isDefaultMethod(),
                method.isEnabled(),
                method.getCreatedAt());
    }

    private PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getOrder().getId(),
                transaction.getCustomer().getId(),
                transaction.getPaymentMethod().getId(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getGatewayResponseCode(),
                transaction.getGatewayMessage(),
                transaction.getProcessedAt());
    }
}
