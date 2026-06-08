package com.silvaldeweb.dto.user;

import java.time.Instant;

import com.silvaldeweb.model.user.Role;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        Boolean active,
        Role role,
        Instant lastLogin,
        Boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
}
