package com.silvaldeweb.service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.silvaldeweb.dto.order.OrderCreateRequest;
import com.silvaldeweb.dto.order.OrderItemRequest;
import com.silvaldeweb.dto.order.OrderPayRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.dto.order.OrderShipRequest;
import com.silvaldeweb.dto.order.OrderUpdateRequest;
import com.silvaldeweb.exception.order.OrderInvalidStateException;
import com.silvaldeweb.exception.order.OrderNotFoundException;
import com.silvaldeweb.exception.product.ProductNotFoundException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.order.Order;
import com.silvaldeweb.model.order.OrderItem;
import com.silvaldeweb.model.order.OrderStatus;
import com.silvaldeweb.model.order.Payment;
import com.silvaldeweb.model.order.PaymentMethod;
import com.silvaldeweb.model.order.PaymentStatus;
import com.silvaldeweb.model.order.Shipment;
import com.silvaldeweb.model.order.ShipmentStatus;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.order.OrderRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private OrderService orderService;

    private User sampleUser(Long id) {
        return User.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .role(Role.USER)
                .active(true)
                .build();
    }

    private User adminActor() {
        return User.builder().id(99L).email("admin@example.com").role(Role.ADMIN).active(true).build();
    }

    private Product sampleProduct(Long id, BigDecimal price) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .price(price)
                .stock(100)
                .active(true)
                .build();
    }

    private Order sampleOrder(Long id, User customer, OrderStatus status) {
        OrderItem item = OrderItem.builder()
                .id(100L)
                .productId(1L)
                .quantity(2)
                .unitPrice(new BigDecimal("10.00"))
                .lineTotal(new BigDecimal("20.00"))
                .build();
        return Order.builder()
                .id(id)
                .orderNumber("ORD-20260101-TEST")
                .customer(customer)
                .status(status)
                .total(new BigDecimal("20.00"))
                .shippingAddress("Calle 1, Madrid")
                .items(new java.util.ArrayList<>(List.of(item)))
                .build();
    }

    @Test
    void createSavesOrderWithCalculatedTotal() {
        User user = sampleUser(1L);
        Product product = sampleProduct(10L, new BigDecimal("10.00"));
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(10L, 2)),
                "Calle Mayor 1, Madrid"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(50L);
            return o;
        });

        OrderResponse response = orderService.create(1L, false, request, user);

        assertEquals(50L, response.id());
        assertEquals(1L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(0, new BigDecimal("20.00").compareTo(response.total()));
        assertEquals(1, response.items().size());
    }

    @Test
    void createThrowsUserNotFoundWhenUserMissing() {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(10L, 1)),
                "Calle 1"
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> orderService.create(99L, false, request, sampleUser(99L)));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createThrowsProductNotFoundWhenItemProductMissing() {
        User user = sampleUser(1L);
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(999L, 1)),
                "Calle 1"
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> orderService.create(1L, false, request, user));
    }

    @Test
    void getThrowsOrderNotFoundWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderService.get(99L, 1L, false));
    }

    @Test
    void getThrowsAccessDeniedWhenUserIsNotOwnerAndNotAdmin() {
        User owner = sampleUser(1L);
        Order order = sampleOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> orderService.get(10L, 2L, false));
    }

    @Test
    void getAllowsAdminToAccessAnyOrder() {
        User owner = sampleUser(1L);
        Order order = sampleOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.get(10L, 99L, true);

        assertEquals(10L, response.id());
        assertEquals(1L, response.customerId());
    }

    @Test
    void payTransitionsPendingToPaidAndStoresPayment() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PENDING);
        Product product = sampleProduct(1L, new BigDecimal("10.00"));
        product.setStock(50);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.pay(10L, 1L, false,
                new OrderPayRequest(PaymentMethod.CARD, "stripe-123"), user);

        assertEquals(OrderStatus.PAID, response.status());
        assertNotNull(response.payment());
        assertEquals(PaymentMethod.CARD, response.payment().method());
        assertEquals(PaymentStatus.CAPTURED, response.payment().status());
    }

    @Test
    void payDecrementsProductStock() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PENDING);
        Product product = sampleProduct(1L, new BigDecimal("10.00"));
        product.setStock(100);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.pay(10L, 1L, false,
                new OrderPayRequest(PaymentMethod.CARD, null), user);

        assertEquals(98, product.getStock());
    }

    @Test
    void payRejectsWhenInsufficientStock() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PENDING);
        Product product = sampleProduct(1L, new BigDecimal("10.00"));
        product.setStock(1);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        assertThrows(com.silvaldeweb.exception.cart.InsufficientStockException.class,
                () -> orderService.pay(10L, 1L, false,
                        new OrderPayRequest(PaymentMethod.CARD, null), user));
    }

    @Test
    void payRejectsWhenOrderNotPending() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.SHIPPED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(OrderInvalidStateException.class,
                () -> orderService.pay(10L, 1L, false, new OrderPayRequest(PaymentMethod.CARD, null), user));
    }

    @Test
    void shipTransitionsPaidToShippedAndStoresShipment() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PAID);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.ship(10L, 1L, false, new OrderShipRequest("SEUR", "TRACK-1"), user);

        assertEquals(OrderStatus.SHIPPED, response.status());
        assertNotNull(response.shipment());
        assertEquals("SEUR", response.shipment().carrier());
        assertEquals("TRACK-1", response.shipment().trackingNumber());
    }

    @Test
    void shipRejectsWhenOrderNotPaid() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(OrderInvalidStateException.class,
                () -> orderService.ship(10L, 1L, false, new OrderShipRequest("SEUR", "T1"), user));
    }

    @Test
    void cancelRejectsWhenAlreadyShipped() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.SHIPPED);
        order.setShipment(Shipment.builder().status(ShipmentStatus.SHIPPED).build());
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(OrderInvalidStateException.class,
                () -> orderService.cancel(10L, 1L, false, user));
    }

    @Test
    void cancelTransitionsPendingToCancelled() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.cancel(10L, 1L, false, user);

        assertEquals(OrderStatus.CANCELLED, response.status());
    }

    @Test
    void updateRejectsWhenNotPending() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PAID);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(OrderInvalidStateException.class,
                () -> orderService.update(10L, 1L, false, new OrderUpdateRequest("Otra calle"), user));
    }

    @Test
    void updateChangesShippingAddressWhenPending() {
        User user = sampleUser(1L);
        Order order = sampleOrder(10L, user, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.update(10L, 1L, false, new OrderUpdateRequest("Nueva calle 5"), user);

        assertEquals("Nueva calle 5", response.shippingAddress());
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderService.delete(99L, adminActor()));
    }

    @Test
    void listReturnsOnlyCustomerOrdersWhenNotAdmin() {
        User user = sampleUser(1L);
        Order o1 = sampleOrder(10L, user, OrderStatus.PENDING);
        when(orderRepository.findByCustomerId(1L)).thenReturn(List.of(o1));

        List<OrderResponse> responses = orderService.list(1L, false, null);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).customerId());
    }

    @Test
    void payRejectsAccessByNonOwner() {
        User owner = sampleUser(1L);
        Order order = sampleOrder(10L, owner, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.pay(10L, 2L, false, new OrderPayRequest(PaymentMethod.CARD, null), sampleUser(2L)));
    }
}
