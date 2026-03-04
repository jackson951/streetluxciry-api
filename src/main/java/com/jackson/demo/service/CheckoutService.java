package com.jackson.demo.service;
import java.util.UUID;

import com.jackson.demo.dto.request.CheckoutSessionPayRequest;
import com.jackson.demo.dto.request.CreateCheckoutSessionRequest;
import com.jackson.demo.dto.request.FinalizeCheckoutSessionRequest;
import com.jackson.demo.dto.response.CheckoutSessionItemResponse;
import com.jackson.demo.dto.response.CheckoutSessionPayResponse;
import com.jackson.demo.dto.response.CheckoutSessionResponse;
import com.jackson.demo.dto.response.FinalizeCheckoutSessionResponse;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.CartItem;
import com.jackson.demo.entity.CheckoutSession;
import com.jackson.demo.entity.CheckoutSessionItem;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.entity.OrderItem;
import com.jackson.demo.entity.PaymentMethod;
import com.jackson.demo.entity.PaymentTransaction;
import com.jackson.demo.entity.Product;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.model.CheckoutSessionStatus;
import com.jackson.demo.model.OrderStatus;
import com.jackson.demo.model.PaymentStatus;
import com.jackson.demo.repository.CartRepository;
import com.jackson.demo.repository.CheckoutSessionRepository;
import com.jackson.demo.repository.CustomerOrderRepository;
import com.jackson.demo.repository.PaymentMethodRepository;
import com.jackson.demo.repository.PaymentTransactionRepository;
import com.jackson.demo.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private static final int SESSION_TTL_MINUTES = 15;
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("350.00");

    private final CartService cartService;
    private final CustomerService customerService;
    private final CartRepository cartRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;

    public CheckoutService(
            CartService cartService,
            CustomerService customerService,
            CartRepository cartRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            PaymentMethodRepository paymentMethodRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository) {
        this.cartService = cartService;
        this.customerService = customerService;
        this.cartRepository = cartRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CheckoutSessionResponse createSession(UUID customerId, CreateCheckoutSessionRequest request) {
        Cart cart = cartService.getOrCreateCart(customerId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        CheckoutSession session = new CheckoutSession();
        session.setCustomer(customerService.findCustomer(customerId));
        session.setStatus(CheckoutSessionStatus.PAYMENT_PENDING);
        session.setExpiresAt(Instant.now().plusSeconds(SESSION_TTL_MINUTES * 60L));
        session.setPaymentIntentRef("pay_" + UUID.randomUUID().toString().replace("-", ""));

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("Inactive product cannot be checked out: " + product.getName());
            }
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            CheckoutSessionItem item = new CheckoutSessionItem();
            item.setCheckoutSession(session);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            session.getItems().add(item);
            total = total.add(subtotal);
        }

        // Calculate delivery fee if applicable
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (request != null && request.isDelivery() != null && request.isDelivery()) {
            deliveryFee = DELIVERY_FEE;
        }
        
        BigDecimal finalTotal = total.add(deliveryFee);
        session.setTotalAmount(finalTotal);
        session.setDeliveryFee(deliveryFee);
        session.setIsDelivery(request != null && request.isDelivery() != null && request.isDelivery());
        session.setShippingAddress(request != null ? request.shippingAddress() : null);
        
        if (request != null && request.preferredPaymentMethodId() != null) {
            validatePaymentMethodOwnership(request.preferredPaymentMethodId(), customerId);
        }
        return toSessionResponse(checkoutSessionRepository.save(session));
    }

    @Transactional
    public CheckoutSessionPayResponse paySession(UUID sessionId, CheckoutSessionPayRequest request) {
        CheckoutSession session = findSessionForUpdate(sessionId);
        validateSessionIsActive(session);

        PaymentTransaction existing = paymentTransactionRepository
                .findByCheckoutSessionIdAndIdempotencyKey(sessionId, request.idempotencyKey())
                .orElse(null);
        if (existing != null) {
            return toPayResponse(session, existing);
        }
        if (session.getStatus() == CheckoutSessionStatus.APPROVED) {
            PaymentTransaction approved = paymentTransactionRepository
                    .findFirstByCheckoutSessionIdAndStatusOrderByProcessedAtDesc(sessionId, PaymentStatus.APPROVED)
                    .orElseThrow(() -> new BadRequestException("Session already approved but payment transaction is missing"));
            return toPayResponse(session, approved);
        }

        PaymentMethod method = findPaymentMethod(session.getCustomer().getId(), request.paymentMethodId());
        if (!method.isEnabled()) {
            return saveDeclined(session, method, request.idempotencyKey(), "METHOD_DISABLED", "Payment method is disabled");
        }
        if (isExpired(method.getExpiryMonth(), method.getExpiryYear())) {
            return saveDeclined(session, method, request.idempotencyKey(), "EXPIRED_CARD", "Payment method is expired");
        }
        if (request.cvv() == null || !request.cvv().matches("^[0-9]{3,4}$")) {
            return saveDeclined(session, method, request.idempotencyKey(), "INVALID_CVV", "Invalid CVV");
        }

        boolean approved = session.getTotalAmount().compareTo(new BigDecimal("5000.00")) <= 0
                && !method.getLast4().equals("0000");
        if (!approved) {
            return saveDeclined(session, method, request.idempotencyKey(), "DECLINED", "Payment declined by issuer");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCheckoutSession(session);
        transaction.setCustomer(session.getCustomer());
        transaction.setPaymentMethod(method);
        transaction.setStatus(PaymentStatus.APPROVED);
        transaction.setAmount(session.getTotalAmount());
        transaction.setCurrency(session.getCurrency());
        transaction.setGatewayResponseCode("APPROVED");
        transaction.setGatewayMessage("Payment approved");
        transaction.setIdempotencyKey(request.idempotencyKey());

        session.setStatus(CheckoutSessionStatus.APPROVED);
        checkoutSessionRepository.save(session);
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        return toPayResponse(session, saved);
    }

    @Transactional
    public FinalizeCheckoutSessionResponse finalizeSession(
            UUID sessionId, FinalizeCheckoutSessionRequest request) {
        CheckoutSession session = findSessionForUpdate(sessionId);
        UUID customerId = session.getCustomer().getId();

        if (Instant.now().isAfter(session.getExpiresAt())) {
            throw new BadRequestException("Checkout session expired. Start checkout again.");
        }
        if (session.getStatus() == CheckoutSessionStatus.CONSUMED && session.getOrder() != null) {
            return new FinalizeCheckoutSessionResponse(session.getOrder().getId());
        }
        if (session.getStatus() != CheckoutSessionStatus.APPROVED) {
            throw new BadRequestException("Checkout session is not approved for finalization");
        }

        if (session.getIdempotencyKey() != null && !session.getIdempotencyKey().equals(request.idempotencyKey())) {
            throw new BadRequestException("Finalize idempotency key does not match previous request");
        }
        session.setIdempotencyKey(request.idempotencyKey());

        PaymentTransaction approvedTransaction = paymentTransactionRepository
                .findFirstByCheckoutSessionIdAndStatusOrderByProcessedAtDesc(sessionId, PaymentStatus.APPROVED)
                .orElseThrow(() -> new BadRequestException("No approved payment for this checkout session"));

        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer: " + customerId));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty. Cannot finalize checkout.");
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(session.getCustomer());
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus(OrderStatus.ORDER_RECEIVED);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProduct().getId()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);
            order.getItems().add(orderItem);
            total = total.add(subtotal);

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        if (session.getTotalAmount().compareTo(total) != 0) {
            throw new BadRequestException("Cart totals changed after payment approval. Please retry checkout.");
        }
        if (approvedTransaction.getAmount().compareTo(total) != 0) {
            throw new BadRequestException("Approved payment amount does not match order total");
        }

        order.setTotalAmount(total);
        order.setDeliveryFee(session.getDeliveryFee());
        order.setIsDelivery(session.getIsDelivery());
        order.setShippingAddress(session.getShippingAddress());
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        approvedTransaction.setOrder(savedOrder);
        paymentTransactionRepository.save(approvedTransaction);

        cart.getItems().clear();
        session.setOrder(savedOrder);
        session.setStatus(CheckoutSessionStatus.CONSUMED);
        checkoutSessionRepository.save(session);
        return new FinalizeCheckoutSessionResponse(savedOrder.getId());
    }

    @Transactional(readOnly = true)
    public CheckoutSessionResponse getSession(UUID sessionId) {
        CheckoutSession session = checkoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found: " + sessionId));
        return toSessionResponse(session);
    }

    private CheckoutSession findSessionForUpdate(UUID sessionId) {
        return checkoutSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found: " + sessionId));
    }

    private void validatePaymentMethodOwnership(UUID paymentMethodId, UUID customerId) {
        findPaymentMethod(customerId, paymentMethodId);
    }

    private PaymentMethod findPaymentMethod(UUID customerId, UUID paymentMethodId) {
        return paymentMethodRepository.findByIdAndCustomerId(paymentMethodId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found: " + paymentMethodId));
    }

    private void validateSessionIsActive(CheckoutSession session) {
        if (session.getStatus() == CheckoutSessionStatus.CONSUMED) {
            throw new BadRequestException("Checkout session already consumed");
        }
        if (Instant.now().isAfter(session.getExpiresAt())) {
            throw new BadRequestException("Checkout session expired. Start checkout again.");
        }
    }

    private CheckoutSessionPayResponse saveDeclined(
            CheckoutSession session,
            PaymentMethod method,
            String idempotencyKey,
            String code,
            String message) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCheckoutSession(session);
        transaction.setCustomer(session.getCustomer());
        transaction.setPaymentMethod(method);
        transaction.setStatus(PaymentStatus.DECLINED);
        transaction.setAmount(session.getTotalAmount());
        transaction.setCurrency(session.getCurrency());
        transaction.setGatewayResponseCode(code);
        transaction.setGatewayMessage(message);
        transaction.setIdempotencyKey(idempotencyKey);
        session.setStatus(CheckoutSessionStatus.FAILED);
        checkoutSessionRepository.save(session);
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        return toPayResponse(session, saved);
    }

    private boolean isExpired(Integer month, Integer year) {
        return YearMonth.of(year, month).isBefore(YearMonth.now());
    }

    private CheckoutSessionResponse toSessionResponse(CheckoutSession session) {
        List<CheckoutSessionItemResponse> items = session.getItems().stream()
                .map(item -> new CheckoutSessionItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        return new CheckoutSessionResponse(
                session.getId(),
                session.getStatus(),
                session.getTotalAmount(),
                session.getCurrency(),
                session.getExpiresAt(),
                session.getPaymentIntentRef(),
                items);
    }

    private CheckoutSessionPayResponse toPayResponse(CheckoutSession session, PaymentTransaction transaction) {
        return new CheckoutSessionPayResponse(
                session.getId(),
                transaction.getId(),
                transaction.getStatus(),
                transaction.getGatewayResponseCode(),
                transaction.getGatewayMessage());
    }

}
