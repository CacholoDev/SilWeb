package com.silvaldeweb.dto.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
        @NotEmpty(message = "El pedido debe contener al menos un item")
        @Size(max = 100, message = "El pedido no puede tener más de 100 items")
        @Valid
        List<OrderItemRequest> items,

        @NotBlank(message = "La dirección de envío es obligatoria")
        @Size(max = 500, message = "La dirección de envío no puede tener más de 500 caracteres")
        String shippingAddress
) {
}
