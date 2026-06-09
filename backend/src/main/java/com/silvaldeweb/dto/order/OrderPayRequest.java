package com.silvaldeweb.dto.order;

import com.silvaldeweb.model.order.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderPayRequest(
        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod method,

        @Size(max = 200, message = "La referencia del proveedor no puede tener más de 200 caracteres")
        String providerReference
) {
}
