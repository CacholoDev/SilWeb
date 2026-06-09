package com.silvaldeweb.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.silvaldeweb.model.order.OrderStatus;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long customerId,
        OrderStatus status,
        BigDecimal total,
        String shippingAddress,
        List<OrderItemResponse> items,
        PaymentResponse payment,
        ShipmentResponse shipment,
        Instant createdAt,
        Instant updatedAt
) {
}
