package com.silvaldeweb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "El email debe tener un TLD válido (ej. user@example.com)")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
