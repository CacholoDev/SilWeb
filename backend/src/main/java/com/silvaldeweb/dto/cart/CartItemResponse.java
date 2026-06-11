package com.silvaldeweb.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Integer availableStock,
        Boolean productActive
) {
}
