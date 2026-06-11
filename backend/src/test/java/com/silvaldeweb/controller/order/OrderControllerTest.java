package com.silvaldeweb.controller.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silvaldeweb.dto.order.OrderCreateRequest;
import com.silvaldeweb.dto.order.OrderItemRequest;
import com.silvaldeweb.dto.order.OrderItemResponse;
import com.silvaldeweb.dto.order.OrderPayRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.dto.order.OrderShipRequest;
import com.silvaldeweb.dto.order.OrderUpdateRequest;
import com.silvaldeweb.dto.order.PaymentResponse;
import com.silvaldeweb.dto.order.ShipmentResponse;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.order.OrderStatus;
import com.silvaldeweb.model.order.PaymentMethod;
import com.silvaldeweb.model.order.PaymentStatus;
import com.silvaldeweb.model.order.ShipmentStatus;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.order.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderController orderController;

    private Authentication adminAuth;
    private Authentication userAuth;
    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = User.builder().id(1L).email("admin@example.com").active(true).build();
        normalUser = User.builder().id(2L).email("customer@example.com").active(true).build();

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        userAuth = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private OrderResponse sampleResponse(Long id) {
        return new OrderResponse(
                id,
                "ORD-20260101-AAAA",
                2L,
                OrderStatus.PENDING,
                new BigDecimal("20.00"),
                "Calle 1, Madrid",
                List.of(new OrderItemResponse(100L, 10L, 2, new BigDecimal("10.00"), new BigDecimal("20.00"))),
                null,
                null,
                Instant.parse("2026-06-09T10:00:00Z"),
                Instant.parse("2026-06-09T10:00:00Z")
        );
    }

    private OrderResponse paidResponse(Long id) {
        return new OrderResponse(
                id, "ORD-20260101-BBBB", 2L, OrderStatus.PAID, new BigDecimal("20.00"),
                "Calle 1, Madrid",
                List.of(new OrderItemResponse(100L, 10L, 2, new BigDecimal("10.00"), new BigDecimal("20.00"))),
                new PaymentResponse(200L, PaymentMethod.CARD, PaymentStatus.CAPTURED, new BigDecimal("20.00"),
                        Instant.parse("2026-06-09T11:00:00Z")),
                null,
                Instant.parse("2026-06-09T10:00:00Z"),
                Instant.parse("2026-06-09T11:00:00Z")
        );
    }

    @Test
    void createReturnsCreatedOrder() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.create(eq(2L), eq(false), any(OrderCreateRequest.class), any()))
                .thenReturn(sampleResponse(50L));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderItemRequest(10L, 2, new BigDecimal("10.00"))),
                "Calle 1, Madrid"
        );

        mockMvc.perform(post("/api/orders")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.customerId").value(2L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productId").value(10L))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void createRejectsEmptyItems() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(List.of(), "Calle 1");

        mockMvc.perform(post("/api/orders")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items"));
    }

    @Test
    void getReturnsOwnOrder() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.get(eq(50L), eq(2L), eq(false))).thenReturn(sampleResponse(50L));

        mockMvc.perform(get("/api/orders/50").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.customerId").value(2L));
    }

    @Test
    void listReturnsOrders() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.list(eq(2L), eq(false), any())).thenReturn(List.of(sampleResponse(50L)));

        mockMvc.perform(get("/api/orders").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50L));
    }

    @Test
    void updateReturnsUpdatedOrder() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.update(eq(50L), eq(2L), eq(false), any(OrderUpdateRequest.class), any()))
                .thenReturn(sampleResponse(50L));

        OrderUpdateRequest request = new OrderUpdateRequest("Nueva calle 5");

        mockMvc.perform(put("/api/orders/50")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    void payReturnsPaidOrder() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.pay(eq(50L), eq(2L), eq(false), any(OrderPayRequest.class), any()))
                .thenReturn(paidResponse(50L));

        OrderPayRequest request = new OrderPayRequest(PaymentMethod.CARD, "stripe-123");

        mockMvc.perform(patch("/api/orders/50/pay")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payment.method").value("CARD"))
                .andExpect(jsonPath("$.payment.status").value("CAPTURED"));
    }

    @Test
    void shipRequiresCarrier() throws Exception {
        OrderShipRequest request = new OrderShipRequest("", "TRACK-OK");

        mockMvc.perform(patch("/api/orders/50/ship")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("carrier"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        doNothing().when(orderService).delete(eq(50L), any());

        mockMvc.perform(delete("/api/orders/50").principal(userAuth))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelReturnsCancelledOrder() throws Exception {
        OrderResponse cancelled = new OrderResponse(
                50L, "ORD-X", 2L, OrderStatus.CANCELLED, new BigDecimal("20.00"),
                "Calle 1", List.of(), null, null,
                Instant.parse("2026-06-09T10:00:00Z"),
                Instant.parse("2026-06-09T10:00:00Z")
        );
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.cancel(eq(50L), eq(2L), eq(false), any())).thenReturn(cancelled);

        mockMvc.perform(patch("/api/orders/50/cancel").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void deliverReturnsDeliveredOrder() throws Exception {
        OrderResponse delivered = new OrderResponse(
                50L, "ORD-X", 2L, OrderStatus.DELIVERED, new BigDecimal("20.00"),
                "Calle 1", List.of(), null,
                new ShipmentResponse(300L, "SEUR", "T1", ShipmentStatus.DELIVERED,
                        Instant.parse("2026-06-09T10:00:00Z"),
                        Instant.parse("2026-06-09T12:00:00Z")),
                Instant.parse("2026-06-09T10:00:00Z"),
                Instant.parse("2026-06-09T12:00:00Z")
        );
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(orderService.markDelivered(eq(50L), eq(2L), eq(false), any())).thenReturn(delivered);

        mockMvc.perform(patch("/api/orders/50/deliver").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.shipment.status").value("DELIVERED"));
    }
}
