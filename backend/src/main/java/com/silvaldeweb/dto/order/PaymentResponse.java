package com.silvaldeweb.dto.order;

import java.math.BigDecimal;
import java.time.Instant;

import com.silvaldeweb.model.order.PaymentMethod;
import com.silvaldeweb.model.order.PaymentStatus;

public record PaymentResponse(
        Long id,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        Instant paidAt
) {
}
