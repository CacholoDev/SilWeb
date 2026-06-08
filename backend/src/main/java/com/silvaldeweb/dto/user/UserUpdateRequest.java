package com.silvaldeweb.dto.user;

import com.silvaldeweb.model.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "El email debe tener un TLD válido (ej. user@example.com)")
        @Size(max = 255, message = "El email no puede tener más de 255 caracteres")
        String email,

        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede tener más de 120 caracteres")
        String name,

        @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos")
        @Size(max = 40, message = "El teléfono no puede tener más de 40 caracteres")
        String phone,

        Boolean active,

        Role role
) {
}
