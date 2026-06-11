package com.silvaldeweb.dto.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank(message = "La dirección de envío es obligatoria")
        @Size(max = 500, message = "La dirección de envío no puede tener más de 500 caracteres")
        String shippingAddress
) {
}
