package com.jackson.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackson.demo.dto.request.CreatePaymentMethodRequest;
import com.jackson.demo.dto.request.ProcessPaymentRequest;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.entity.PaymentMethod;
import com.jackson.demo.model.OrderStatus;
import com.jackson.demo.model.PaymentStatus;
import com.jackson.demo.repository.CustomerOrderRepository;
import com.jackson.demo.repository.PaymentMethodRepository;
import com.jackson.demo.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    private PaymentService paymentService;
    private CustomerService customerService;
    private Customer knownCustomer;

    @BeforeEach
    void setUp() {
        knownCustomer = new Customer();
        ReflectionTestUtils.setField(knownCustomer, "id", 7L);
        customerService = new CustomerService(null, null) {
            @Override
            public Customer findCustomer(Long id) {
                return knownCustomer;
            }
        };
        paymentService = new PaymentService(
                customerService, customerOrderRepository, paymentMethodRepository, paymentTransactionRepository);
    }

    @Test
    void createPaymentMethodSetsLast4AndDefaultForFirstMethod() {
        when(paymentMethodRepository.countByCustomerId(7L)).thenReturn(0L);
        when(paymentMethodRepository.findByCustomerIdAndDefaultMethodTrue(7L)).thenReturn(Optional.empty());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> {
            PaymentMethod saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 55L);
            return saved;
        });

        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest(
                null,
                "Demo Customer",
                "4111 1111 1111 1234",
                null,
                12,
                YearMonth.now().plusYears(2).getYear(),
                "742 Evergreen Terrace",
                null);

        var response = paymentService.createPaymentMethod(7L, request);

        assertEquals(55L, response.id());
        assertEquals("VISA", response.brand());
        assertEquals("1234", response.last4());
        assertTrue(response.defaultMethod());
    }

    @Test
    void processOrderPaymentApprovesAndMarksOrderPaid() {
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setCustomer(knownCustomer);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("199.99"));

        PaymentMethod method = new PaymentMethod();
        ReflectionTestUtils.setField(method, "id", 5L);
        method.setCustomer(knownCustomer);
        method.setEnabled(true);
        method.setExpiryMonth(12);
        method.setExpiryYear(YearMonth.now().plusYears(2).getYear());
        method.setLast4("4242");

        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentMethodRepository.findById(5L)).thenReturn(Optional.of(method));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.processOrderPayment(10L, new ProcessPaymentRequest(5L, "123"));

        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void processOrderPaymentDeclinesWhenCvvInvalid() {
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setCustomer(knownCustomer);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("50.00"));

        PaymentMethod method = new PaymentMethod();
        ReflectionTestUtils.setField(method, "id", 5L);
        method.setCustomer(knownCustomer);
        method.setEnabled(true);
        method.setExpiryMonth(12);
        method.setExpiryYear(YearMonth.now().plusYears(2).getYear());
        method.setLast4("4242");

        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentMethodRepository.findById(5L)).thenReturn(Optional.of(method));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.processOrderPayment(10L, new ProcessPaymentRequest(5L, "12"));

        assertEquals(PaymentStatus.DECLINED, response.status());
        assertEquals("INVALID_CVV", response.gatewayResponseCode());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }
}
