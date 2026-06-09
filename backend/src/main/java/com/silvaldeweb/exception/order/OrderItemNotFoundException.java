package com.silvaldeweb.exception.order;

public class OrderItemNotFoundException extends RuntimeException {

    public OrderItemNotFoundException(Long id) {
        super("Order item with id " + id + " was not found.");
    }
}
