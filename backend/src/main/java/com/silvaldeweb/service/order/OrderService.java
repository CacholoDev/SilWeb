package com.silvaldeweb.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.order.OrderCreateRequest;
import com.silvaldeweb.dto.order.OrderItemRequest;
import com.silvaldeweb.dto.order.OrderItemResponse;
import com.silvaldeweb.dto.order.OrderPayRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.dto.order.OrderShipRequest;
import com.silvaldeweb.dto.order.OrderUpdateRequest;
import com.silvaldeweb.dto.order.PaymentResponse;
import com.silvaldeweb.dto.order.ShipmentResponse;
import com.silvaldeweb.exception.order.OrderAlreadyExistsException;
import com.silvaldeweb.exception.order.OrderInvalidStateException;
import com.silvaldeweb.exception.order.OrderNotFoundException;
import com.silvaldeweb.exception.product.ProductNotFoundException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.order.Order;
import com.silvaldeweb.model.order.OrderItem;
import com.silvaldeweb.model.order.OrderStatus;
import com.silvaldeweb.model.order.Payment;
import com.silvaldeweb.model.order.PaymentStatus;
import com.silvaldeweb.model.order.Shipment;
import com.silvaldeweb.model.order.ShipmentStatus;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.order.OrderRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter ORDER_NUMBER_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_ORDER_NUMBER_RETRIES = 5;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public OrderResponse create(Long authenticatedUserId, boolean isAdmin, OrderCreateRequest request, User actor) {
        User customer = resolveCustomer(authenticatedUserId, isAdmin, request);
        log.info("Creating order customerId={} itemCount={}", customer.getId(), request.items().size());

        Order order = Order.builder()
                .orderNumber(generateUniqueOrderNumber())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .total(BigDecimal.ZERO)
                .shippingAddress(request.shippingAddress())
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));

            BigDecimal lineTotal = itemRequest.unitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .lineTotal(lineTotal)
                    .build();
            order.getItems().add(item);

            total = total.add(lineTotal);
        }
        order.setTotal(total);

        Order saved = orderRepository.save(order);
        auditLogService.record(actor, Action.CREATE, "Order", saved.getId(),
                "orderNumber=" + saved.getOrderNumber() + " total=" + saved.getTotal()
                        + " items=" + saved.getItems().size(), null);
        log.info("Order created id={} orderNumber='{}' total={}",
                saved.getId(), saved.getOrderNumber(), saved.getTotal());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(Long authenticatedUserId, boolean isAdmin, OrderStatus status) {
        log.info("Listing orders userId={} isAdmin={} status={}", authenticatedUserId, isAdmin, status);
        List<Order> orders = isAdmin
                ? (status == null ? orderRepository.findAll() : orderRepository.findAll().stream()
                        .filter(o -> o.getStatus() == status).toList())
                : (status == null
                        ? orderRepository.findByCustomerId(authenticatedUserId)
                        : orderRepository.findByCustomerIdAndStatus(authenticatedUserId, status));
        log.info("Found {} orders", orders.size());
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, Long authenticatedUserId, boolean isAdmin) {
        log.info("Getting order id={} userId={} isAdmin={}", id, authenticatedUserId, isAdmin);
        Order order = findById(id);
        enforceOwnership(order, authenticatedUserId, isAdmin);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse update(Long id, Long authenticatedUserId, boolean isAdmin, OrderUpdateRequest request, User actor) {
        log.info("Updating order id={} userId={} isAdmin={}", id, authenticatedUserId, isAdmin);
        Order order = findById(id);
        enforceOwnership(order, authenticatedUserId, isAdmin);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderInvalidStateException(
                    "Only PENDING orders can be updated. Current status: " + order.getStatus());
        }
        if (request.shippingAddress() != null && !request.shippingAddress().isBlank()) {
            String previous = order.getShippingAddress();
            order.setShippingAddress(request.shippingAddress());
            auditLogService.record(actor, Action.UPDATE, "Order", order.getId(),
                    "shippingAddress: " + previous + " -> " + request.shippingAddress(), null);
        }

        Order saved = orderRepository.save(order);
        log.info("Order updated id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse pay(Long id, Long authenticatedUserId, boolean isAdmin, OrderPayRequest request, User actor) {
        log.info("Paying order id={} userId={} method={}", id, authenticatedUserId, request.method());
        Order order = findById(id);
        enforceOwnership(order, authenticatedUserId, isAdmin);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderInvalidStateException(
                    "Only PENDING orders can be paid. Current status: " + order.getStatus());
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(request.method())
                .status(PaymentStatus.CAPTURED)
                .amount(order.getTotal())
                .providerReference(request.providerReference())
                .paidAt(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                .build();
        order.setPayment(payment);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.PAID);

        Order saved = orderRepository.save(order);
        auditLogService.record(actor, Action.STATE_CHANGE, "Order", saved.getId(),
                previousStatus + " -> PAID via " + request.method(), null);
        log.info("Order paid id={} amount={}", saved.getId(), saved.getTotal());
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse ship(Long id, Long authenticatedUserId, boolean isAdmin, OrderShipRequest request, User actor) {
        log.info("Shipping order id={} userId={} carrier='{}'", id, authenticatedUserId, request.carrier());
        Order order = findById(id);
        enforceOwnership(order, authenticatedUserId, isAdmin);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderInvalidStateException(
                    "Only PAID orders can be shipped. Current status: " + order.getStatus());
        }

        Shipment shipment = Shipment.builder()
                .order(order)
                .carrier(request.carrier())
                .trackingNumber(request.trackingNumber())
                .status(ShipmentStatus.SHIPPED)
                .shippedAt(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                .build();
        order.setShipment(shipment);
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.SHIPPED);

        Order saved = orderRepository.save(order);
        auditLogService.record(actor, Action.STATE_CHANGE, "Order", saved.getId(),
                previousStatus + " -> SHIPPED carrier=" + request.carrier() + " tracking=" + request.trackingNumber(), null);
        log.info("Order shipped id={} trackingNumber='{}'", saved.getId(),
                saved.getShipment() != null ? saved.getShipment().getTrackingNumber() : null);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse markDelivered(Long id, Long authenticatedUserId, boolean isAdmin, User actor) {
        log.info("Marking order delivered id={} userId={} isAdmin={}", id, authenticatedUserId, isAdmin);
        Order order = findById(id);
        enforceOwnership(order, authenticatedUserId, isAdmin);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new OrderInvalidStateException(
                    "Only SHIPPED orders can be delivered. Current status: " + order.getStatus());
        }
        if (order.getShipment() == null) {
            throw new OrderInvalidStateException("Order has no shipment to mark as delivered.");
        }

        order.getShipment().setStatus(ShipmentStatus.DELIVERED);
        order.getShipment().setDeliveredAt(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);

        Order saved = orderRepository.save(order);
        auditLogService.record(actor, Action.STATE_CHANGE, "Order", saved.getId(),
                previousStatus + " -> DELIVERED", null);
        log.info("Order delivered id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse cancel(Long id, Long authenticatedUserId, boolean isAdmin, User actor) {
        log.info("Cancelling order id={} userId={} isAdmin={}", id, authenticatedUserId, isAdmin);
        Order order = findById(id);
        enforceOwnership(order, authenticatedUserId, isAdmin);

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new OrderInvalidStateException(
                    "Cannot cancel an order in status " + order.getStatus());
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderInvalidStateException("Order is already cancelled.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        auditLogService.record(actor, Action.STATE_CHANGE, "Order", saved.getId(),
                previousStatus + " -> CANCELLED", null);
        log.info("Order cancelled id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, User actor) {
        log.info("Deleting order id={} actor={}", id, actor != null ? actor.getEmail() : "null");
        try {
            Order order = findById(id);
            String metadata = "orderNumber=" + order.getOrderNumber()
                    + " customerId=" + (order.getCustomer() != null ? order.getCustomer().getId() : null)
                    + " total=" + order.getTotal();
            orderRepository.delete(order);
            auditLogService.record(actor, Action.DELETE, "Order", id, metadata, null);
            log.info("Order id={} deleted", id);
        } catch (OrderNotFoundException exception) {
            log.warn("Delete failed: order id={} not found", id);
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Delete failed for order id={}", id, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private User resolveCustomer(Long authenticatedUserId, boolean isAdmin, OrderCreateRequest request) {
        if (isAdmin) {
            return userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new UserNotFoundException(authenticatedUserId));
        }
        return userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new UserNotFoundException(authenticatedUserId));
    }

    private void enforceOwnership(Order order, Long authenticatedUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!order.getCustomer().getId().equals(authenticatedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only access your own orders.");
        }
    }

    private String generateUniqueOrderNumber() {
        for (int attempt = 1; attempt <= MAX_ORDER_NUMBER_RETRIES; attempt++) {
            String candidate = "ORD-" + OffsetDateTime.now(ZoneOffset.UTC).format(ORDER_NUMBER_TIMESTAMP)
                    + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
            log.warn("Order number collision on attempt {}: {}", attempt, candidate);
        }
        throw new OrderAlreadyExistsException("ORD-collision-after-retries");
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getStatus(),
                order.getTotal(),
                order.getShippingAddress(),
                order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(this::toItemResponse).toList(),
                order.getPayment() == null ? null : toPaymentResponse(order.getPayment()),
                order.getShipment() == null ? null : toShipmentResponse(order.getShipment()),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getPaidAt()
        );
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getCarrier(),
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt()
        );
    }
}
