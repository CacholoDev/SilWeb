package com.silvaldeweb.exception.order;

public class OrderInvalidStateException extends RuntimeException {

    public OrderInvalidStateException(String message) {
        super(message);
    }
}
