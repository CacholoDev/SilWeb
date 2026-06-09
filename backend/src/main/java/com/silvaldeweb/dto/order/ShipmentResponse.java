package com.silvaldeweb.dto.order;

import java.time.Instant;

import com.silvaldeweb.model.order.ShipmentStatus;

public record ShipmentResponse(
        Long id,
        String carrier,
        String trackingNumber,
        ShipmentStatus status,
        Instant shippedAt,
        Instant deliveredAt
) {
}
