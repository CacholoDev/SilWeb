package com.silvaldeweb.dto.order;

import jakarta.validation.constraints.Size;

public record OrderUpdateRequest(
        @Size(max = 500, message = "La dirección de envío no puede tener más de 500 caracteres")
        String shippingAddress
) {
}
