package com.jackson.demo.service;
import java.util.UUID;

import com.jackson.demo.dto.response.OrderTrackingResponse;
import com.jackson.demo.dto.response.OrderTrackingStageResponse;
import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.CartItem;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.entity.OrderItem;
import com.jackson.demo.entity.Product;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.mapper.ApiMapper;
import com.jackson.demo.model.OrderStatus;
import com.jackson.demo.repository.CustomerOrderRepository;
import com.jackson.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
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
        return ApiMapper.toOrderResponse(findOrder(orderId));
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        CustomerOrder order = findOrder(orderId);
        OrderStatus currentStatus = order.getStatus();

        if (newStatus == null) {
            throw new BadRequestException("Order status is required");
        }
        if (newStatus == currentStatus) {
            return ApiMapper.toOrderResponse(order);
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled orders cannot be moved to another status");
        }
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered orders cannot be moved to another status");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled status is not part of tracking flow");
        }

        List<OrderStatus> flow = trackingFlow();
        int currentIndex = flow.indexOf(currentStatus);
        int targetIndex = flow.indexOf(newStatus);
        if (currentIndex < 0 || targetIndex < 0) {
            throw new BadRequestException("Only tracking statuses can be updated through this endpoint");
        }
        if (targetIndex != currentIndex + 1) {
            throw new BadRequestException("Order status must move to the next stage only");
        }

        order.setStatus(newStatus);
        return ApiMapper.toOrderResponse(customerOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse getOrderTracking(UUID orderId) {
        CustomerOrder order = findOrder(orderId);
        List<OrderStatus> flow = trackingFlow();
        int currentIndex = flow.indexOf(order.getStatus());
        List<OrderTrackingStageResponse> stages = flow.stream()
                .map(status -> {
                    int step = flow.indexOf(status) + 1;
                    int statusIndex = flow.indexOf(status);
                    boolean completed = currentIndex >= 0 && statusIndex <= currentIndex;
                    boolean current = currentIndex >= 0 && statusIndex == currentIndex;
                    return new OrderTrackingStageResponse(step, status, toTrackingLabel(status), completed, current);
                })
                .toList();
        return new OrderTrackingResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCreatedAt(),
                stages);
    }

    @SuppressWarnings("null")
    private CustomerOrder findOrder(UUID orderId) {
        return customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private List<OrderStatus> trackingFlow() {
        return List.of(
                OrderStatus.ORDER_RECEIVED,
                OrderStatus.PROCESSING_PACKING,
                OrderStatus.SHIPPED,
                OrderStatus.IN_TRANSIT,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED);
    }

    private String toTrackingLabel(OrderStatus status) {
        return switch (status) {
            case ORDER_RECEIVED -> "Order Received";
            case PROCESSING_PACKING -> "Processing/Packing";
            case SHIPPED -> "Shipped";
            case IN_TRANSIT -> "In Transit";
            case OUT_FOR_DELIVERY -> "Out for Delivery";
            case DELIVERED -> "Delivered";
            default -> status.name();
        };
    }
}
