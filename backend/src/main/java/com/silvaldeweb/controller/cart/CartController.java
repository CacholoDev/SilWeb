package com.silvaldeweb.controller.cart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.config.AuthUtils;
import com.silvaldeweb.dto.cart.AddCartItemRequest;
import com.silvaldeweb.dto.cart.CartResponse;
import com.silvaldeweb.dto.cart.CheckoutRequest;
import com.silvaldeweb.dto.cart.UpdateCartItemRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.cart.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    public CartResponse get(Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        log.info("GET /api/carts userId={}", user.getId());
        return cartService.getActiveCart(user.getId());
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request,
                                Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        log.info("POST /api/carts/items userId={} productId={} quantity={}",
                user.getId(), request.productId(), request.quantity());
        return cartService.addItem(user.getId(), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(@PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest request,
                                   Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        log.info("PATCH /api/carts/items/{} userId={} newQuantity={}", itemId, user.getId(), request.quantity());
        return cartService.updateItem(user.getId(), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long itemId, Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        log.info("DELETE /api/carts/items/{} userId={}", itemId, user.getId());
        return cartService.removeItem(user.getId(), itemId);
    }

    @DeleteMapping
    public CartResponse clear(Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        log.info("DELETE /api/carts userId={}", user.getId());
        return cartService.clear(user.getId());
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request,
                                  Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("POST /api/carts/checkout userId={} isAdmin={}", user.getId(), isAdmin);
        return cartService.checkout(user.getId(), isAdmin, request.shippingAddress());
    }
}
