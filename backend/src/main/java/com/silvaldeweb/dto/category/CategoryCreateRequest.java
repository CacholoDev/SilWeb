package com.silvaldeweb.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede tener más de 500 caracteres")
        String description,

        Boolean active
) {
}
