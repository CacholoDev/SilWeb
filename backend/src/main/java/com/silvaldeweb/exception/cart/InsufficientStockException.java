package com.silvaldeweb.exception.cart;

import java.util.Map;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(Long productId, Integer requested, Integer available) {
        super(
                HttpStatus.CONFLICT,
                "Insufficient stock",
                "Product " + productId + ": requested " + requested + " but only " + available + " available.",
                Map.of("productId", productId, "requested", requested, "available", available)
        );
    }
}
