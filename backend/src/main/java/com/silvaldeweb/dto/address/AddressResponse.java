package com.silvaldeweb.dto.address;

import java.time.Instant;

public record AddressResponse(
        Long id,
        String fullName,
        String street,
        String city,
        String province,
        String postalCode,
        String country,
        Boolean isDefault,
        Long userId,
        Instant createdAt,
        Instant updatedAt
) {
}
