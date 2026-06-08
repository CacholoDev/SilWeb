package com.silvaldeweb.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 160, message = "El nombre no puede tener más de 160 caracteres")
        String name,

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 60, message = "El SKU no puede tener más de 60 caracteres")
        String sku,

        @Size(max = 1000, message = "La descripción no puede tener más de 1000 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
        BigDecimal price,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
        Integer stock,

        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        Boolean active
) {
}
