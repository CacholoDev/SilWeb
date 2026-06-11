package com.silvaldeweb.exception.cart;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(Long id) {
        super("Cart with id " + id + " was not found.");
    }
}
