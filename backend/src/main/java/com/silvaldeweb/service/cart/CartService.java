package com.silvaldeweb.service.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.cart.AddCartItemRequest;
import com.silvaldeweb.dto.cart.CartItemResponse;
import com.silvaldeweb.dto.cart.CartResponse;
import com.silvaldeweb.dto.cart.UpdateCartItemRequest;
import com.silvaldeweb.dto.order.OrderCreateRequest;
import com.silvaldeweb.dto.order.OrderItemRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.exception.cart.InsufficientStockException;
import com.silvaldeweb.exception.product.ProductNotFoundException;
import com.silvaldeweb.model.cart.Cart;
import com.silvaldeweb.model.cart.CartItem;
import com.silvaldeweb.model.cart.CartStatus;
import com.silvaldeweb.model.product.Product;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.cart.CartRepository;
import com.silvaldeweb.repository.product.ProductRepository;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.order.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public CartResponse getActiveCart(Long authenticatedUserId) {
        log.info("Getting active cart userId={}", authenticatedUserId);
        Cart cart = getOrCreateCart(authenticatedUserId);
        return toResponse(cart, loadProductsForCart(cart));
    }

    @Transactional
    public CartResponse addItem(Long authenticatedUserId, AddCartItemRequest request) {
        log.info("Adding item to cart userId={} productId={} quantity={}",
                authenticatedUserId, request.productId(), request.quantity());
        Cart cart = getOrCreateCart(authenticatedUserId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        if (!product.getActive()) {
            throw new ProductNotFoundException(product.getId());
        }

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
            log.info("Merged into existing cart item id={} newQuantity={}",
                    existing.getId(), existing.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .quantity(request.quantity())
                    .build();
            cart.getItems().add(newItem);
            log.info("Added new cart item productId={} quantity={}", product.getId(), request.quantity());
        }

        if (cart.getStatus() != CartStatus.ACTIVE) {
            cart.setStatus(CartStatus.ACTIVE);
        }

        Cart saved = cartRepository.save(cart);
        return toResponse(saved, Map.of(product.getId(), product));
    }

    @Transactional
    public CartResponse updateItem(Long authenticatedUserId, Long itemId, UpdateCartItemRequest request) {
        log.info("Updating cart item userId={} itemId={} newQuantity={}",
                authenticatedUserId, itemId, request.quantity());
        Cart cart = getOrCreateCart(authenticatedUserId);
        CartItem item = findOwnedCartItem(cart, itemId);
        item.setQuantity(request.quantity());
        Cart saved = cartRepository.save(cart);
        return toResponse(saved, loadProductsForCart(saved));
    }

    @Transactional
    public CartResponse removeItem(Long authenticatedUserId, Long itemId) {
        log.info("Removing cart item userId={} itemId={}", authenticatedUserId, itemId);
        Cart cart = getOrCreateCart(authenticatedUserId);
        CartItem item = findOwnedCartItem(cart, itemId);
        cart.getItems().remove(item);
        Cart saved = cartRepository.save(cart);
        log.info("Cart item id={} removed from cart id={}", itemId, saved.getId());
        return toResponse(saved, loadProductsForCart(saved));
    }

    @Transactional
    public CartResponse clear(Long authenticatedUserId) {
        log.info("Clearing cart userId={}", authenticatedUserId);
        Cart cart = getOrCreateCart(authenticatedUserId);
        cart.getItems().clear();
        if (cart.getStatus() == CartStatus.CONVERTED) {
            cart.setStatus(CartStatus.ACTIVE);
        }
        Cart saved = cartRepository.save(cart);
        return toResponse(saved, Map.of());
    }

    @Transactional
    public OrderResponse checkout(Long authenticatedUserId, boolean isAdmin, String shippingAddress) {
        log.info("Checkout userId={} isAdmin={}", authenticatedUserId, isAdmin);
        Cart cart = getOrCreateCart(authenticatedUserId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new com.silvaldeweb.exception.order.OrderInvalidStateException(
                    "Cart is empty. Add items before checkout.");
        }

        Map<Long, Product> products = loadProductsForCart(cart);

        for (CartItem item : cart.getItems()) {
            Product product = products.get(item.getProductId());
            if (product == null || !product.getActive()) {
                throw new ProductNotFoundException(item.getProductId());
            }
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException(product.getId(), item.getQuantity(), product.getStock());
            }
        }

        List<OrderItemRequest> orderItems = cart.getItems().stream()
                .map(item -> new OrderItemRequest(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        OrderCreateRequest orderRequest = new OrderCreateRequest(orderItems, shippingAddress);
        User actor = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new com.silvaldeweb.exception.user.UserNotFoundException(authenticatedUserId));
        OrderResponse order = orderService.create(authenticatedUserId, isAdmin, orderRequest, actor);

        cart.getItems().clear();
        cart.setStatus(CartStatus.CONVERTED);
        cartRepository.save(cart);

        log.info("Checkout done cartId={} orderId={} orderNumber='{}' total={}",
                cart.getId(), order.id(), order.orderNumber(), order.total());

        return order;
    }

    private Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            log.info("Creating new cart for userId={}", customerId);
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new com.silvaldeweb.exception.user.UserNotFoundException(customerId));
            Cart newCart = Cart.builder()
                    .customer(customer)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    private CartItem findOwnedCartItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new com.silvaldeweb.exception.cart.CartItemNotFoundException(itemId));
    }

    private Map<Long, Product> loadProductsForCart(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return Map.of();
        }
        List<Long> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
    }

    private CartResponse toResponse(Cart cart, Map<Long, Product> products) {
        List<CartItemResponse> itemResponses = cart.getItems() == null ? List.of()
                : cart.getItems().stream()
                        .map(item -> toItemResponse(item, products.get(item.getProductId())))
                        .toList();
        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(
                cart.getId(),
                cart.getCustomer() != null ? cart.getCustomer().getId() : null,
                cart.getStatus(),
                itemResponses,
                total,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItem item, Product product) {
        BigDecimal unitPrice = product != null ? product.getPrice() : null;
        BigDecimal lineTotal = unitPrice == null
                ? null
                : unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                product != null ? product.getName() : null,
                item.getQuantity(),
                unitPrice,
                lineTotal,
                product != null ? product.getStock() : null,
                product != null ? product.getActive() : null
        );
    }
}
