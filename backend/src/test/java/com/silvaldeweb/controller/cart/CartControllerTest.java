package com.silvaldeweb.controller.cart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.silvaldeweb.dto.cart.AddCartItemRequest;
import com.silvaldeweb.dto.cart.CartItemResponse;
import com.silvaldeweb.dto.cart.CartResponse;
import com.silvaldeweb.dto.cart.CheckoutRequest;
import com.silvaldeweb.dto.cart.UpdateCartItemRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.model.order.OrderStatus;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.cart.CartStatus;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.cart.CartService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartController cartController;

    private Authentication userAuth;
    private User normalUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        normalUser = User.builder().id(2L).email("customer@example.com").active(true).build();
        userAuth = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private CartResponse sampleCart(Long id) {
        return new CartResponse(
                id, 2L, CartStatus.ACTIVE,
                List.of(new CartItemResponse(50L, 20L, "Product 20", 3,
                        new BigDecimal("10.00"), new BigDecimal("30.00"), 100, true)),
                new BigDecimal("30.00"),
                Instant.parse("2026-06-09T10:00:00Z"),
                Instant.parse("2026-06-09T10:00:00Z")
        );
    }

    @Test
    void getReturnsActiveCart() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(cartService.getActiveCart(2L)).thenReturn(sampleCart(99L));

        mockMvc.perform(get("/api/carts").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.customerId").value(2L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].productId").value(20L))
                .andExpect(jsonPath("$.items[0].productName").value("Product 20"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(30.00))
                .andExpect(jsonPath("$.total").value(30.00));
    }

    @Test
    void addItemReturnsUpdatedCart() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(cartService.addItem(eq(2L), any(AddCartItemRequest.class))).thenReturn(sampleCart(99L));

        AddCartItemRequest request = new AddCartItemRequest(20L, 3);

        mockMvc.perform(post("/api/carts/items")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(20L))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void addItemRejectsQuantityZero() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(20L, 0);

        mockMvc.perform(post("/api/carts/items")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quantity"));
    }

    @Test
    void addItemRejectsMissingProductId() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(null, 3);

        mockMvc.perform(post("/api/carts/items")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("productId"));
    }

    @Test
    void updateItemReturnsUpdatedCart() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(cartService.updateItem(eq(2L), eq(50L), any(UpdateCartItemRequest.class)))
                .thenReturn(sampleCart(99L));

        UpdateCartItemRequest request = new UpdateCartItemRequest(7);

        mockMvc.perform(patch("/api/carts/items/50")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void removeItemReturnsCart() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(cartService.removeItem(eq(2L), eq(50L))).thenReturn(sampleCart(99L));

        mockMvc.perform(delete("/api/carts/items/50").principal(userAuth))
                .andExpect(status().isOk());
    }

    @Test
    void clearReturnsEmptyCart() throws Exception {
        CartResponse empty = new CartResponse(99L, 2L, CartStatus.ACTIVE, List.of(), BigDecimal.ZERO,
                Instant.parse("2026-06-09T10:00:00Z"),
                Instant.parse("2026-06-09T10:00:00Z"));
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(cartService.clear(2L)).thenReturn(empty);

        mockMvc.perform(delete("/api/carts").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void checkoutReturnsCreatedOrder() throws Exception {
        when(userRepository.findByEmailIgnoreCase("customer@example.com"))
                .thenReturn(java.util.Optional.of(normalUser));
        when(cartService.checkout(eq(2L), eq(false), any())).thenReturn(
                new OrderResponse(77L, "ORD-X", 2L, OrderStatus.PENDING,
                        new BigDecimal("30.00"), "Calle 1", List.of(), null, null, null, null));

        CheckoutRequest request = new CheckoutRequest("Calle Mayor 1, 28001 Madrid");

        mockMvc.perform(post("/api/carts/checkout")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(30.00));
    }

    @Test
    void checkoutRejectsBlankShippingAddress() throws Exception {
        CheckoutRequest request = new CheckoutRequest("");

        mockMvc.perform(post("/api/carts/checkout")
                .principal(userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("shippingAddress"));
    }
}
