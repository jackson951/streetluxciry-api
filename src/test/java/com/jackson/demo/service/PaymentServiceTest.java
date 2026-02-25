package com.jackson.demo.service;
import java.util.UUID;

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
    private UUID knownCustomerId;

    @BeforeEach
    void setUp() {
        knownCustomerId = UUID.randomUUID();
        knownCustomer = new Customer();
        ReflectionTestUtils.setField(knownCustomer, "id", knownCustomerId);
        customerService = new CustomerService(null, null) {
            @Override
            public Customer findCustomer(UUID id) {
                return knownCustomer;
            }
        };
        paymentService = new PaymentService(
                customerService, customerOrderRepository, paymentMethodRepository, paymentTransactionRepository);
    }

    @SuppressWarnings("null")
    @Test
    void createPaymentMethodSetsLast4AndDefaultForFirstMethod() {
        UUID paymentMethodId = UUID.randomUUID();
        when(paymentMethodRepository.countByCustomerId(knownCustomerId)).thenReturn(0L);
        when(paymentMethodRepository.findByCustomerIdAndDefaultMethodTrue(knownCustomerId)).thenReturn(Optional.empty());
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(invocation -> {
            PaymentMethod saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", paymentMethodId);
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

        var response = paymentService.createPaymentMethod(knownCustomerId, request);

        assertEquals(paymentMethodId, response.id());
        assertEquals("VISA", response.brand());
        assertEquals("1234", response.last4());
        assertTrue(response.defaultMethod());
    }

    @SuppressWarnings("null")
    @Test
    void processOrderPaymentApprovesAndMarksOrderPaid() {
        UUID orderId = UUID.randomUUID();
        UUID paymentMethodId = UUID.randomUUID();
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", orderId);
        order.setCustomer(knownCustomer);
        order.setStatus(OrderStatus.ORDER_RECEIVED);
        order.setTotalAmount(new BigDecimal("199.99"));

        PaymentMethod method = new PaymentMethod();
        ReflectionTestUtils.setField(method, "id", paymentMethodId);
        method.setCustomer(knownCustomer);
        method.setEnabled(true);
        method.setExpiryMonth(12);
        method.setExpiryYear(YearMonth.now().plusYears(2).getYear());
        method.setLast4("4242");

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentMethodRepository.findById(paymentMethodId)).thenReturn(Optional.of(method));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.processOrderPayment(orderId, new ProcessPaymentRequest(paymentMethodId, "123"));

        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals(OrderStatus.ORDER_RECEIVED, order.getStatus());
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @SuppressWarnings("null")
    @Test
    void processOrderPaymentDeclinesWhenCvvInvalid() {
        UUID orderId = UUID.randomUUID();
        UUID paymentMethodId = UUID.randomUUID();
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", orderId);
        order.setCustomer(knownCustomer);
        order.setStatus(OrderStatus.ORDER_RECEIVED);
        order.setTotalAmount(new BigDecimal("50.00"));

        PaymentMethod method = new PaymentMethod();
        ReflectionTestUtils.setField(method, "id", paymentMethodId);
        method.setCustomer(knownCustomer);
        method.setEnabled(true);
        method.setExpiryMonth(12);
        method.setExpiryYear(YearMonth.now().plusYears(2).getYear());
        method.setLast4("4242");

        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentMethodRepository.findById(paymentMethodId)).thenReturn(Optional.of(method));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.processOrderPayment(orderId, new ProcessPaymentRequest(paymentMethodId, "12"));

        assertEquals(PaymentStatus.DECLINED, response.status());
        assertEquals("INVALID_CVV", response.gatewayResponseCode());
        assertEquals(OrderStatus.ORDER_RECEIVED, order.getStatus());
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }
}
