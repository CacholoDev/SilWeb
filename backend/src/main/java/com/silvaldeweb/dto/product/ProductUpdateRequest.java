package com.silvaldeweb.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 60) String sku,
        @Size(max = 1000) String description,
        @NotNull @Positive BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotNull Long categoryId,
        Boolean active
) {
}
