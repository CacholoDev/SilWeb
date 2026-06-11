package com.silvaldeweb.service.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.silvaldeweb.dto.cart.AddCartItemRequest;
import com.silvaldeweb.dto.cart.CartItemResponse;
import com.silvaldeweb.dto.cart.CartResponse;
import com.silvaldeweb.dto.cart.UpdateCartItemRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.model.order.OrderStatus;
import com.silvaldeweb.exception.cart.CartItemNotFoundException;
import com.silvaldeweb.exception.cart.InsufficientStockException;
import com.silvaldeweb.exception.order.OrderInvalidStateException;
import com.silvaldeweb.exception.product.ProductNotFoundException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.cart.Cart;
import com.silvaldeweb.model.cart.CartItem;
import com.silvaldeweb.model.cart.CartStatus;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.cart.CartItemRepository;
import com.silvaldeweb.repository.cart.CartRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.order.OrderService;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CartService cartService;

    private User sampleUser(Long id) {
        return User.builder().id(id).email("user" + id + "@example.com").role(Role.USER).active(true).build();
    }

    private Product sampleProduct(Long id, BigDecimal price, int stock) {
        return Product.builder().id(id).name("Product " + id).price(price).stock(stock).active(true).build();
    }

    private Cart emptyCart(Long id, User user) {
        return Cart.builder().id(id).customer(user).status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
    }

    @Test
    void getActiveCartCreatesNewCartIfNotExists() {
        User user = sampleUser(1L);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            c.setId(99L);
            return c;
        });

        CartResponse response = cartService.getActiveCart(1L);

        assertEquals(99L, response.id());
        assertEquals(1L, response.customerId());
        assertEquals(CartStatus.ACTIVE, response.status());
        assertEquals(0, response.items().size());
    }

    @Test
    void getActiveCartReturnsExistingEmptyCart() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getActiveCart(1L);

        assertEquals(10L, response.id());
        assertEquals(0, response.items().size());
    }

    @Test
    void addItemCreatesNewCartItem() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        Product product = sampleProduct(20L, new BigDecimal("10.00"), 100);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            if (!c.getItems().isEmpty()) {
                c.getItems().get(0).setId(50L);
            }
            return c;
        });

        CartResponse response = cartService.addItem(1L, new AddCartItemRequest(20L, 3));

        assertEquals(1, response.items().size());
        CartItemResponse item = response.items().get(0);
        assertEquals(20L, item.productId());
        assertEquals("Product 20", item.productName());
        assertEquals(3, item.quantity());
        assertEquals(0, new BigDecimal("10.00").compareTo(item.unitPrice()));
        assertEquals(0, new BigDecimal("30.00").compareTo(item.lineTotal()));
        assertEquals(0, new BigDecimal("30.00").compareTo(response.total()));
    }

    @Test
    void addItemMergesQuantityIfProductAlreadyInCart() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        cart.getItems().add(CartItem.builder().id(50L).cart(cart).productId(20L).quantity(2).build());
        Product product = sampleProduct(20L, new BigDecimal("10.00"), 100);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.addItem(1L, new AddCartItemRequest(20L, 3));

        assertEquals(1, response.items().size());
        assertEquals(5, response.items().get(0).quantity());
    }

    @Test
    void addItemThrowsProductNotFoundWhenProductMissing() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> cartService.addItem(1L, new AddCartItemRequest(999L, 1)));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemRejectsInactiveProduct() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        Product product = sampleProduct(20L, new BigDecimal("10.00"), 100);
        product.setActive(false);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class,
                () -> cartService.addItem(1L, new AddCartItemRequest(20L, 1)));
    }

    @Test
    void updateItemChangesQuantity() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        cart.getItems().add(CartItem.builder().id(50L).cart(cart).productId(20L).quantity(2).build());
        Product product = sampleProduct(20L, new BigDecimal("10.00"), 100);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllById(List.of(20L))).thenReturn(List.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.updateItem(1L, 50L, new UpdateCartItemRequest(7));

        assertEquals(7, response.items().get(0).quantity());
    }

    @Test
    void updateItemThrowsWhenItemNotInCart() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class,
                () -> cartService.updateItem(1L, 999L, new UpdateCartItemRequest(3)));
    }

    @Test
    void removeItemDeletesItemFromCart() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        cart.getItems().add(CartItem.builder().id(50L).cart(cart).productId(20L).quantity(2).build());
        cart.getItems().add(CartItem.builder().id(51L).cart(cart).productId(21L).quantity(1).build());
        Product p21 = sampleProduct(21L, new BigDecimal("5.00"), 100);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllById(List.of(21L))).thenReturn(List.of(p21));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.removeItem(1L, 50L);

        assertEquals(1, response.items().size());
        assertEquals(21L, response.items().get(0).productId());
    }

    @Test
    void clearEmptiesCart() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        cart.getItems().add(CartItem.builder().id(50L).cart(cart).productId(20L).quantity(2).build());
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.clear(1L);

        assertEquals(0, response.items().size());
    }

    @Test
    void checkoutCreatesOrderAndMarksCartAsConverted() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        cart.getItems().add(CartItem.builder().id(50L).cart(cart).productId(20L).quantity(2).build());
        Product product = sampleProduct(20L, new BigDecimal("10.00"), 100);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllById(List.of(20L))).thenReturn(List.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderService.create(eq(1L), eq(false), any(), any())).thenReturn(
                new OrderResponse(77L, "ORD-X", 1L, OrderStatus.PENDING,
                        new BigDecimal("20.00"), "addr", List.of(), null, null, null, null));

        OrderResponse response = cartService.checkout(1L, false, "Calle 1");

        assertEquals(77L, response.id());
        assertEquals(OrderStatus.PENDING, response.status());
        verify(orderService).create(eq(1L), eq(false), any(), eq(user));
    }

    @Test
    void checkoutThrowsWhenCartEmpty() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        assertThrows(OrderInvalidStateException.class,
                () -> cartService.checkout(1L, false, "Calle 1"));
    }

    @Test
    void checkoutThrowsInsufficientStock() {
        User user = sampleUser(1L);
        Cart cart = emptyCart(10L, user);
        cart.getItems().add(CartItem.builder().id(50L).cart(cart).productId(20L).quantity(10).build());
        Product product = sampleProduct(20L, new BigDecimal("10.00"), 3);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findAllById(List.of(20L))).thenReturn(List.of(product));

        assertThrows(InsufficientStockException.class,
                () -> cartService.checkout(1L, false, "Calle 1"));
    }

    @Test
    void getOrCreateThrowsUserNotFoundWhenUserMissing() {
        when(cartRepository.findByCustomerId(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> cartService.getActiveCart(99L));
    }
}
