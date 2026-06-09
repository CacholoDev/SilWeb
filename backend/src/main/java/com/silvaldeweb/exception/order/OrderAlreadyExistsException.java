package com.silvaldeweb.exception.order;

public class OrderAlreadyExistsException extends RuntimeException {

    public OrderAlreadyExistsException(String orderNumber) {
        super("Order with number '" + orderNumber + "' already exists.");
    }
}
