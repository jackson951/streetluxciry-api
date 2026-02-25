package com.jackson.demo.service;

import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.CartItem;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.entity.OrderItem;
import com.jackson.demo.entity.Product;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.mapper.ApiMapper;
import com.jackson.demo.repository.CustomerOrderRepository;
import com.jackson.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final CartService cartService;
    private final CustomerService customerService;
    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;

    public OrderService(
            CartService cartService,
            CustomerService customerService,
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository) {
        this.cartService = cartService;
        this.customerService = customerService;
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse checkout(UUID customerId) {
        Cart cart = cartService.getOrCreateCart(customerId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customerService.findCustomer(customerId));
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setSubtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            order.getItems().add(orderItem);
            total = total.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(total);
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        cart.getItems().clear();
        return ApiMapper.toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listCustomerOrders(UUID customerId) {
        customerService.findCustomer(customerId);
        return customerOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(ApiMapper::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAllOrders() {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ApiMapper::toOrderResponse)
                .toList();
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return ApiMapper.toOrderResponse(order);
    }
}
