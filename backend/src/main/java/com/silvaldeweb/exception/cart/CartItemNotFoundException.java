package com.silvaldeweb.exception.cart;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long id) {
        super("Cart item with id " + id + " was not found.");
    }
}
