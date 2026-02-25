package com.jackson.demo.service;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.model.OrderStatus;
import com.jackson.demo.repository.CustomerOrderRepository;
import com.jackson.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        CartService cartService = new CartService(null, null, null, null);
        CustomerService customerService = new CustomerService(null, null);
        orderService = new OrderService(cartService, customerService, customerOrderRepository, productRepository);
    }

    @Test
    void updateOrderStatusMovesToNextStageOnly() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = buildOrder(orderId, OrderStatus.ORDER_RECEIVED);
        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING_PACKING);

        assertEquals(OrderStatus.PROCESSING_PACKING, response.status());
        verify(customerOrderRepository).save(order);
    }

    @Test
    void updateOrderStatusRejectsSkippedStage() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = buildOrder(orderId, OrderStatus.ORDER_RECEIVED);
        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED));
    }

    @Test
    void getOrderTrackingReturnsSixStages() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = buildOrder(orderId, OrderStatus.IN_TRANSIT);
        when(customerOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        var tracking = orderService.getOrderTracking(orderId);

        assertEquals(6, tracking.stages().size());
        assertEquals(OrderStatus.IN_TRANSIT, tracking.currentStatus());
        assertEquals(4, tracking.stages().stream().filter(stage -> stage.completed()).count());
    }

    private CustomerOrder buildOrder(UUID orderId, OrderStatus status) {
        Customer customer = new Customer();
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());

        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "createdAt", Instant.now());
        order.setCustomer(customer);
        order.setOrderNumber("ORD-TEST123");
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(status);
        return order;
    }
}
