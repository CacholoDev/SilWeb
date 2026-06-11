package com.silvaldeweb.exception.cart;

public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final Integer requested;
    private final Integer available;

    public InsufficientStockException(Long productId, Integer requested, Integer available) {
        super("Product " + productId + ": requested " + requested + " but only " + available + " available.");
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getRequested() {
        return requested;
    }

    public Integer getAvailable() {
        return available;
    }
}
