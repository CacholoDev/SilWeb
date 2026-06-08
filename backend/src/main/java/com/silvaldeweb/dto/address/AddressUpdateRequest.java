package com.silvaldeweb.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres")
        String fullName,

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 200, message = "La calle no puede tener más de 200 caracteres")
        String street,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 100, message = "La ciudad no puede tener más de 100 caracteres")
        String city,

        @NotBlank(message = "La provincia es obligatoria")
        @Size(max = 100, message = "La provincia no puede tener más de 100 caracteres")
        String province,

        @NotBlank(message = "El código postal es obligatorio")
        @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe tener 5 dígitos")
        String postalCode,

        @NotBlank(message = "El país es obligatorio")
        @Size(max = 100, message = "El país no puede tener más de 100 caracteres")
        String country,

        Boolean isDefault,

        @NotNull(message = "El usuario es obligatorio")
        Long userId
) {
}
