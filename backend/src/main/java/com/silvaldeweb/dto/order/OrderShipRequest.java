package com.silvaldeweb.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderShipRequest(
        @NotBlank(message = "El transportista es obligatorio")
        @Size(max = 80, message = "El transportista no puede tener más de 80 caracteres")
        String carrier,

        @NotBlank(message = "El número de seguimiento es obligatorio")
        @Size(max = 120, message = "El número de seguimiento no puede tener más de 120 caracteres")
        String trackingNumber
) {
}
