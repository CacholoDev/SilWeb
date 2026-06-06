package com.silvaldeweb.dto.category;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
