package com.silvaldeweb.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        BigDecimal price,
        Integer stock,
        Boolean active,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
}
