package com.silvaldeweb.dto.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.silvaldeweb.model.cart.CartStatus;

public record CartResponse(
        Long id,
        Long customerId,
        CartStatus status,
        List<CartItemResponse> items,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
}
