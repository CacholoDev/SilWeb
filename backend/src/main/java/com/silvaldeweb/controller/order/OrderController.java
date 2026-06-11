package com.silvaldeweb.controller.order;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.config.AuthUtils;
import com.silvaldeweb.dto.order.OrderCreateRequest;
import com.silvaldeweb.dto.order.OrderPayRequest;
import com.silvaldeweb.dto.order.OrderResponse;
import com.silvaldeweb.dto.order.OrderShipRequest;
import com.silvaldeweb.dto.order.OrderUpdateRequest;
import com.silvaldeweb.model.order.OrderStatus;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.order.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request,
                                                Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("POST /api/orders userId={} isAdmin={} itemCount={}",
                actor.getId(), isAdmin, request.items().size());
        OrderResponse response = orderService.create(actor.getId(), isAdmin, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                    Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("GET /api/orders userId={} isAdmin={} status={}", actor.getId(), isAdmin, status);
        return orderService.list(actor.getId(), isAdmin, status);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("GET /api/orders/{} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return orderService.get(id, actor.getId(), isAdmin);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id,
                                @Valid @RequestBody OrderUpdateRequest request,
                                Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("PUT /api/orders/{} userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return orderService.update(id, actor.getId(), isAdmin, request, actor);
    }

    @PatchMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable Long id,
                             @Valid @RequestBody OrderPayRequest request,
                             Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("PATCH /api/orders/{}/pay userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return orderService.pay(id, actor.getId(), isAdmin, request, actor);
    }

    @PatchMapping("/{id}/ship")
    public OrderResponse ship(@PathVariable Long id,
                              @Valid @RequestBody OrderShipRequest request,
                              Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("PATCH /api/orders/{}/ship userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return orderService.ship(id, actor.getId(), isAdmin, request, actor);
    }

    @PatchMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("PATCH /api/orders/{}/deliver userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return orderService.markDelivered(id, actor.getId(), isAdmin, actor);
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        boolean isAdmin = AuthUtils.isAdmin(authentication);
        log.info("PATCH /api/orders/{}/cancel userId={} isAdmin={}", id, actor.getId(), isAdmin);
        return orderService.cancel(id, actor.getId(), isAdmin, actor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User actor = AuthUtils.currentUser(authentication, userRepository);
        log.info("DELETE /api/orders/{} actorId={}", id, actor.getId());
        orderService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }
}
